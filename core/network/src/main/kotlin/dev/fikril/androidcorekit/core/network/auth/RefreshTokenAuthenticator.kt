package dev.fikril.androidcorekit.core.network.auth

import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import dev.fikril.androidcorekit.core.network.telemetry.NetworkRefreshOutcome
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryEvent
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryEventType
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryObserver
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetrySanitizer
import dev.fikril.androidcorekit.core.network.telemetry.NoOpNetworkTelemetryObserver
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class RefreshTokenAuthenticator(
    private val accessTokenProvider: AccessTokenProvider = NoOpAccessTokenProvider,
    private val accessTokenRefresher: AccessTokenRefresher = NoOpAccessTokenRefresher,
    private val telemetryObserver: NetworkTelemetryObserver = NoOpNetworkTelemetryObserver,
    private val maxFollowUpCount: Int = DEFAULT_MAX_FOLLOW_UP_COUNT,
) : Authenticator {
    private val refreshMutex = Mutex()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        val request = response.request
        val requestId = request.header(NetworkHeaders.REQUEST_ID)
        val attemptCount = responseCount(response)
        val requestToken = request.bearerTokenOrNull()
        val canAttemptRefreshRetry =
            attemptCount < maxFollowUpCount &&
                requestToken != null &&
                shouldRetryRequest(request)
        if (!canAttemptRefreshRetry) {
            emitRefreshResult(
                response = response,
                requestId = requestId,
                attemptCount = attemptCount,
                outcome = NetworkRefreshOutcome.SKIPPED,
                reasonCode = "REFRESH_SKIPPED",
                reasonMessage = "Refresh skipped for non-retryable request or missing token.",
            )
            return null
        }

        emitRefreshAttempt(
            response = response,
            requestId = requestId,
            attemptCount = attemptCount,
        )

        val resolution = resolveRetryToken(requestToken = checkNotNull(requestToken))

        val finalToken =
            resolution.token?.takeUnless { token ->
                token == requestToken
            }

        val refreshOutcome =
            if (finalToken != null) {
                NetworkRefreshOutcome.SUCCESS
            } else {
                NetworkRefreshOutcome.FAILURE
            }
        val fallbackReason =
            if (refreshOutcome == NetworkRefreshOutcome.SUCCESS) {
                "REFRESH_SUCCESS"
            } else {
                "REFRESH_NO_NEW_TOKEN"
            }
        emitRefreshResult(
            response = response,
            requestId = requestId,
            attemptCount = attemptCount,
            outcome = refreshOutcome,
            reasonCode = resolution.reasonCode ?: fallbackReason,
            reasonMessage = resolution.reasonMessage,
        )

        return finalToken?.let { token -> request.withBearerToken(token) }
    }

    private fun shouldRetryRequest(request: Request): Boolean {
        val method = request.method.uppercase()
        val hasIdempotencyKey = !request.header(NetworkHeaders.IDEMPOTENCY_KEY).isNullOrBlank()
        val isSafeMethod = method in SAFE_RETRY_METHODS
        val isRetryableWriteMethod = method in WRITE_RETRY_METHODS && hasIdempotencyKey
        return isSafeMethod || isRetryableWriteMethod
    }

    private fun resolveRetryToken(requestToken: String): RefreshResolution {
        val latestToken = accessTokenProvider.accessToken()?.takeIf { it.isNotBlank() }
        val tokenAlreadyRefreshed = latestToken?.takeIf { token -> token != requestToken }
        if (tokenAlreadyRefreshed != null) {
            return RefreshResolution(
                token = tokenAlreadyRefreshed,
                reasonCode = "TOKEN_ALREADY_REFRESHED",
                reasonMessage = "Token was already refreshed by another request.",
            )
        }
        return refreshTokenWithSingleFlight(requestToken = requestToken)
    }

    private fun refreshTokenWithSingleFlight(requestToken: String): RefreshResolution {
        val refreshedToken =
            runCatching {
                runBlocking {
                    refreshMutex.withLock {
                        val tokenAfterLock = accessTokenProvider.accessToken()?.takeIf { it.isNotBlank() }
                        if (tokenAfterLock != null && tokenAfterLock != requestToken) {
                            return@withLock tokenAfterLock
                        }
                        if (!accessTokenRefresher.refreshAccessToken()) {
                            return@withLock null
                        }
                        accessTokenProvider.accessToken()?.takeIf { it.isNotBlank() }
                    }
                }
            }.getOrElse { throwable ->
                return RefreshResolution(
                    token = null,
                    reasonCode = "REFRESH_EXCEPTION",
                    reasonMessage = throwable.message,
                )
            }

        return if (refreshedToken != null) {
            RefreshResolution(
                token = refreshedToken,
                reasonCode = "REFRESH_SUCCESS",
                reasonMessage = null,
            )
        } else {
            RefreshResolution(
                token = null,
                reasonCode = "REFRESH_FAILED",
                reasonMessage = "Access token refresh returned false or empty token.",
            )
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count += 1
            priorResponse = priorResponse.priorResponse
        }
        return count
    }

    private fun Request.withBearerToken(token: String): Request =
        newBuilder()
            .header(NetworkHeaders.AUTHORIZATION, "Bearer $token")
            .build()

    private fun Request.bearerTokenOrNull(): String? =
        header(NetworkHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotBlank() }

    private fun emitRefreshAttempt(
        response: Response,
        requestId: String?,
        attemptCount: Int,
    ) {
        telemetryObserver.onEvent(
            NetworkTelemetryEvent(
                eventType = NetworkTelemetryEventType.REFRESH_ATTEMPT,
                method = response.request.method,
                route = NetworkTelemetrySanitizer.sanitizeRoute(response.request.url.toString()),
                requestId = requestId,
                statusCode = response.code,
                attemptCount = attemptCount,
                errorCode = "REFRESH_ATTEMPT",
            ),
        )
    }

    private fun emitRefreshResult(
        response: Response,
        requestId: String?,
        attemptCount: Int,
        outcome: NetworkRefreshOutcome,
        reasonCode: String,
        reasonMessage: String?,
    ) {
        telemetryObserver.onEvent(
            NetworkTelemetryEvent(
                eventType = NetworkTelemetryEventType.REFRESH_RESULT,
                method = response.request.method,
                route = NetworkTelemetrySanitizer.sanitizeRoute(response.request.url.toString()),
                requestId = requestId,
                statusCode = response.code,
                attemptCount = attemptCount,
                refreshOutcome = outcome,
                errorCode = reasonCode,
                errorMessage = NetworkTelemetrySanitizer.sanitizeText(reasonMessage),
            ),
        )
    }

    private data class RefreshResolution(
        val token: String?,
        val reasonCode: String?,
        val reasonMessage: String?,
    )

    companion object {
        private const val DEFAULT_MAX_FOLLOW_UP_COUNT = 2
        private val SAFE_RETRY_METHODS = setOf("GET", "HEAD", "OPTIONS")
        private val WRITE_RETRY_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}
