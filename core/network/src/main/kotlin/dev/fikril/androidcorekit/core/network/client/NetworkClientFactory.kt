package dev.fikril.androidcorekit.core.network.client

import dev.fikril.androidcorekit.core.network.auth.AccessTokenProvider
import dev.fikril.androidcorekit.core.network.auth.AccessTokenRefresher
import dev.fikril.androidcorekit.core.network.auth.NoOpAccessTokenProvider
import dev.fikril.androidcorekit.core.network.auth.NoOpAccessTokenRefresher
import dev.fikril.androidcorekit.core.network.auth.RefreshTokenAuthenticator
import dev.fikril.androidcorekit.core.network.interceptor.AuthorizationHeaderInterceptor
import dev.fikril.androidcorekit.core.network.interceptor.RequestIdInterceptor
import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class NetworkClientFactory(
    private val accessTokenProvider: AccessTokenProvider = NoOpAccessTokenProvider,
    private val accessTokenRefresher: AccessTokenRefresher = NoOpAccessTokenRefresher,
    private val enableBasicLogging: Boolean = false,
) {
    fun create(): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(RequestIdInterceptor())
            .addInterceptor(AuthorizationHeaderInterceptor(accessTokenProvider))
            .addInterceptor(createLoggingInterceptor())
            .authenticator(
                RefreshTokenAuthenticator(
                    accessTokenProvider = accessTokenProvider,
                    accessTokenRefresher = accessTokenRefresher,
                ),
            ).build()

    private fun createLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level =
                if (enableBasicLogging) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            redactHeader(NetworkHeaders.AUTHORIZATION)
        }
}
