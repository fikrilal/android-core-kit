package dev.fikril.androidcorekit.core.network.interceptor

import dev.fikril.androidcorekit.core.network.client.NetworkRetryPolicy
import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import dev.fikril.androidcorekit.core.network.telemetry.NetworkOutcome
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryEvent
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryEventType
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryObserver
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetrySanitizer
import dev.fikril.androidcorekit.core.network.telemetry.NoOpNetworkTelemetryObserver
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min

class RetryPolicyInterceptor(
    private val policy: NetworkRetryPolicy = NetworkRetryPolicy(),
    private val telemetryObserver: NetworkTelemetryObserver = NoOpNetworkTelemetryObserver,
    private val sleeper: (Long) -> Unit = { delayMillis ->
        if (delayMillis > 0L) {
            TimeUnit.MILLISECONDS.sleep(delayMillis)
        }
    },
    private val nanoTimeProvider: () -> Long = System::nanoTime,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestId = request.header(NetworkHeaders.REQUEST_ID)
        val startNanos = nanoTimeProvider()
        var attempt = 0
        var lastFailure: IOException? = null

        while (attempt < policy.maxAttempts) {
            attempt += 1
            try {
                val response = chain.proceed(request)
                if (!shouldRetryHttp(request = request, statusCode = response.code, attempt = attempt)) {
                    emitEvent(
                        eventType = NetworkTelemetryEventType.REQUEST_COMPLETE,
                        request = request,
                        requestId = requestId,
                        traceId = response.header(NetworkHeaders.REQUEST_ID),
                        outcome = if (response.isSuccessful) NetworkOutcome.SUCCESS else NetworkOutcome.HTTP_ERROR,
                        statusCode = response.code,
                        attempts = attempt,
                        startNanos = startNanos,
                        errorCode = if (response.isSuccessful) null else "HTTP_${response.code}",
                    )
                    return response
                }
                emitEvent(
                    eventType = NetworkTelemetryEventType.RETRY_ATTEMPT,
                    request = request,
                    requestId = requestId,
                    traceId = response.header(NetworkHeaders.REQUEST_ID),
                    outcome = NetworkOutcome.HTTP_ERROR,
                    statusCode = response.code,
                    attempts = attempt,
                    startNanos = startNanos,
                    errorCode = "RETRYABLE_HTTP_${response.code}",
                    errorMessage = "Retrying request after HTTP ${response.code}.",
                )
                response.close()
            } catch (exception: IOException) {
                if (!shouldRetryTransport(request = request, attempt = attempt)) {
                    emitEvent(
                        eventType = NetworkTelemetryEventType.REQUEST_COMPLETE,
                        request = request,
                        requestId = requestId,
                        traceId = null,
                        outcome = NetworkOutcome.NETWORK_ERROR,
                        statusCode = null,
                        attempts = attempt,
                        startNanos = startNanos,
                        errorCode = NETWORK_IO_FAILURE_CODE,
                        errorMessage = exception.message,
                    )
                    throw exception
                }
                emitEvent(
                    eventType = NetworkTelemetryEventType.RETRY_ATTEMPT,
                    request = request,
                    requestId = requestId,
                    traceId = null,
                    outcome = NetworkOutcome.NETWORK_ERROR,
                    statusCode = null,
                    attempts = attempt,
                    startNanos = startNanos,
                    errorCode = NETWORK_IO_RETRY_CODE,
                    errorMessage = exception.message,
                )
                lastFailure = exception
            }

            if (attempt < policy.maxAttempts) {
                sleeper(backoffDelayMillis(attempt))
            }
        }

        emitEvent(
            eventType = NetworkTelemetryEventType.REQUEST_COMPLETE,
            request = request,
            requestId = requestId,
            traceId = null,
            outcome = NetworkOutcome.NETWORK_ERROR,
            statusCode = null,
            attempts = attempt,
            startNanos = startNanos,
            errorCode = RETRY_EXHAUSTED_CODE,
            errorMessage = lastFailure?.message ?: "Retry exhausted.",
        )
        throw lastFailure ?: IOException("Retry exhausted without response.")
    }

    private fun shouldRetryHttp(
        request: Request,
        statusCode: Int,
        attempt: Int,
    ): Boolean =
        attempt < policy.maxAttempts &&
            isRetryableRequest(request) &&
            statusCode in policy.retryableStatusCodes

    private fun shouldRetryTransport(
        request: Request,
        attempt: Int,
    ): Boolean = attempt < policy.maxAttempts && isRetryableRequest(request)

    private fun isRetryableRequest(request: Request): Boolean {
        val method = request.method.uppercase()
        if (method in SAFE_METHODS) return true
        return method in WRITE_METHODS && !request.header(NetworkHeaders.IDEMPOTENCY_KEY).isNullOrBlank()
    }

    private fun backoffDelayMillis(attempt: Int): Long {
        if (policy.initialBackoffMillis == 0L) return 0L
        val multiplier = 1L shl (attempt - 1).coerceAtMost(MAX_BACKOFF_SHIFT)
        val candidateDelay = policy.initialBackoffMillis * multiplier
        return min(candidateDelay, policy.maxBackoffMillis)
    }

    private fun emitEvent(
        eventType: NetworkTelemetryEventType,
        request: Request,
        requestId: String?,
        traceId: String?,
        outcome: NetworkOutcome,
        statusCode: Int?,
        attempts: Int,
        startNanos: Long,
        errorCode: String? = null,
        errorMessage: String? = null,
    ) {
        val elapsedNanos = nanoTimeProvider() - startNanos
        val durationMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos.coerceAtLeast(0L))
        telemetryObserver.onEvent(
            NetworkTelemetryEvent(
                eventType = eventType,
                method = request.method,
                route = NetworkTelemetrySanitizer.sanitizeRoute(request.url.toString()),
                requestId = requestId,
                traceId = traceId,
                outcome = outcome,
                statusCode = statusCode,
                attemptCount = attempts,
                durationMillis = durationMillis,
                errorCode = errorCode,
                errorMessage = NetworkTelemetrySanitizer.sanitizeText(errorMessage),
            ),
        )
    }

    private companion object {
        private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS")
        private val WRITE_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        private const val MAX_BACKOFF_SHIFT = 20
        private const val NETWORK_IO_RETRY_CODE = "RETRYABLE_NETWORK_IO"
        private const val NETWORK_IO_FAILURE_CODE = "NETWORK_IO_FAILURE"
        private const val RETRY_EXHAUSTED_CODE = "RETRY_EXHAUSTED"
    }
}
