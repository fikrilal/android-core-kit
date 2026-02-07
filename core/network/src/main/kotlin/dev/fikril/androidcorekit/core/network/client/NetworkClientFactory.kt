package dev.fikril.androidcorekit.core.network.client

import dev.fikril.androidcorekit.core.network.auth.AccessTokenProvider
import dev.fikril.androidcorekit.core.network.auth.AccessTokenRefresher
import dev.fikril.androidcorekit.core.network.auth.NoOpAccessTokenProvider
import dev.fikril.androidcorekit.core.network.auth.NoOpAccessTokenRefresher
import dev.fikril.androidcorekit.core.network.auth.RefreshTokenAuthenticator
import dev.fikril.androidcorekit.core.network.interceptor.AuthorizationHeaderInterceptor
import dev.fikril.androidcorekit.core.network.interceptor.RequestIdInterceptor
import dev.fikril.androidcorekit.core.network.interceptor.RetryPolicyInterceptor
import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryObserver
import dev.fikril.androidcorekit.core.network.telemetry.NoOpNetworkTelemetryObserver
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class NetworkClientFactory(
    private val accessTokenProvider: AccessTokenProvider = NoOpAccessTokenProvider,
    private val accessTokenRefresher: AccessTokenRefresher = NoOpAccessTokenRefresher,
    private val enableBasicLogging: Boolean = false,
    private val clientConfig: NetworkClientConfig = NetworkClientConfig(),
    private val retryPolicy: NetworkRetryPolicy = NetworkRetryPolicy(),
    private val telemetryObserver: NetworkTelemetryObserver = NoOpNetworkTelemetryObserver,
) {
    fun create(): OkHttpClient =
        OkHttpClient
            .Builder()
            .retryOnConnectionFailure(false)
            .connectionPool(
                ConnectionPool(
                    clientConfig.maxIdleConnections,
                    clientConfig.keepAliveDurationMinutes,
                    TimeUnit.MINUTES,
                ),
            ).connectTimeout(clientConfig.connectTimeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(clientConfig.readTimeoutMillis, TimeUnit.MILLISECONDS)
            .writeTimeout(clientConfig.writeTimeoutMillis, TimeUnit.MILLISECONDS)
            .callTimeout(clientConfig.callTimeoutMillis, TimeUnit.MILLISECONDS)
            .addInterceptor(RequestIdInterceptor())
            .addInterceptor(AuthorizationHeaderInterceptor(accessTokenProvider))
            .addInterceptor(
                RetryPolicyInterceptor(
                    policy = retryPolicy,
                    telemetryObserver = telemetryObserver,
                ),
            ).addInterceptor(createLoggingInterceptor())
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
