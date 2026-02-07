package dev.fikril.androidcorekit.core.network.auth

import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
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
    private val maxFollowUpCount: Int = DEFAULT_MAX_FOLLOW_UP_COUNT,
) : Authenticator {
    private val refreshMutex = Mutex()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        val request = response.request
        val requestToken = request.bearerTokenOrNull()
        val canAttemptRefreshRetry =
            responseCount(response) < maxFollowUpCount &&
                requestToken != null &&
                shouldRetryRequest(request)

        val retryToken =
            requestToken
                ?.takeIf { canAttemptRefreshRetry }
                ?.let { token -> resolveRetryToken(requestToken = token) }

        val finalToken =
            retryToken?.takeUnless { token ->
                token == requestToken
            }
        return finalToken?.let { token -> request.withBearerToken(token) }
    }

    private fun shouldRetryRequest(request: Request): Boolean {
        val method = request.method.uppercase()
        val hasIdempotencyKey = !request.header(NetworkHeaders.IDEMPOTENCY_KEY).isNullOrBlank()
        val isSafeMethod = method in SAFE_RETRY_METHODS
        val isRetryableWriteMethod = method in WRITE_RETRY_METHODS && hasIdempotencyKey
        return isSafeMethod || isRetryableWriteMethod
    }

    private fun resolveRetryToken(requestToken: String): String? {
        val latestToken = accessTokenProvider.accessToken()?.takeIf { it.isNotBlank() }
        val tokenAlreadyRefreshed = latestToken?.takeIf { token -> token != requestToken }
        return tokenAlreadyRefreshed ?: refreshTokenWithSingleFlight(requestToken = requestToken)
    }

    private fun refreshTokenWithSingleFlight(requestToken: String): String? {
        val refreshSucceeded =
            runBlocking {
                refreshMutex.withLock {
                    val tokenAfterLock = accessTokenProvider.accessToken()?.takeIf { it.isNotBlank() }
                    val tokenAlreadyRefreshed = tokenAfterLock != null && tokenAfterLock != requestToken
                    if (tokenAlreadyRefreshed) {
                        true
                    } else {
                        accessTokenRefresher.refreshAccessToken()
                    }
                }
            }

        return if (refreshSucceeded) {
            accessTokenProvider.accessToken()?.takeIf { it.isNotBlank() }
        } else {
            null
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

    companion object {
        private const val DEFAULT_MAX_FOLLOW_UP_COUNT = 2
        private val SAFE_RETRY_METHODS = setOf("GET", "HEAD", "OPTIONS")
        private val WRITE_RETRY_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}
