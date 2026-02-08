package dev.fikril.androidcorekit.core.network.interceptor

import dev.fikril.androidcorekit.core.network.auth.AccessTokenProvider
import dev.fikril.androidcorekit.core.network.auth.NoOpAccessTokenProvider
import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import okhttp3.Interceptor
import okhttp3.Response

class AuthorizationHeaderInterceptor(
    private val accessTokenProvider: AccessTokenProvider = NoOpAccessTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requiresAuth =
            originalRequest
                .header(NetworkHeaders.REQUIRES_AUTH)
                ?.toBooleanStrictOrNull()
                ?: true

        val requestBuilder =
            originalRequest
                .newBuilder()
                .removeHeader(NetworkHeaders.REQUIRES_AUTH)

        if (requiresAuth && originalRequest.header(NetworkHeaders.AUTHORIZATION).isNullOrBlank()) {
            accessTokenProvider.accessToken()?.takeIf { it.isNotBlank() }?.let { token ->
                requestBuilder.header(NetworkHeaders.AUTHORIZATION, "Bearer $token")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
