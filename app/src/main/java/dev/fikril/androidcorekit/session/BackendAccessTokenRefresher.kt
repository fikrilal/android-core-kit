package dev.fikril.androidcorekit.session

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.fikril.androidcorekit.core.network.auth.AccessTokenRefresher
import dev.fikril.androidcorekit.core.network.client.ApiBaseUrlProvider
import dev.fikril.androidcorekit.core.network.client.NetworkClientConfig
import dev.fikril.androidcorekit.core.network.client.applyNetworkSecurityConfig
import dev.fikril.androidcorekit.core.network.execution.NetworkCallExecutor
import dev.fikril.androidcorekit.core.network.interceptor.RequestIdInterceptor
import dev.fikril.androidcorekit.core.network.model.ApiHost
import dev.fikril.androidcorekit.core.network.model.ApiResponse
import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import dev.fikril.androidcorekit.core.network.serialization.defaultNetworkJson
import dev.fikril.androidcorekit.core.network.serialization.mapParser
import dev.fikril.androidcorekit.core.session.SessionManager
import dev.fikril.androidcorekit.core.session.SessionTokens
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendAccessTokenRefresher
    @Inject
    constructor(
        private val sessionManager: SessionManager,
        private val baseUrlProvider: ApiBaseUrlProvider,
        private val networkCallExecutor: NetworkCallExecutor,
        private val clientConfig: NetworkClientConfig = NetworkClientConfig(),
    ) : AccessTokenRefresher {
        private val refreshApi: RefreshApi by lazy { createRefreshApi() }

        override suspend fun refreshAccessToken(): Boolean {
            val refreshToken = sessionManager.refreshToken().orEmpty()
            if (refreshToken.isBlank()) return false

            val response = executeRefresh(refreshToken = refreshToken)
            return applyRefreshResponse(response = response)
        }

        private fun createRefreshApi(): RefreshApi {
            val baseUrl = normalizeBaseUrl(baseUrlProvider.baseUrl(ApiHost.AUTH))
            checkNotNull(baseUrl.toHttpUrlOrNull()) { "Invalid refresh base URL." }

            val client =
                OkHttpClient
                    .Builder()
                    .retryOnConnectionFailure(false)
                    .connectTimeout(clientConfig.connectTimeoutMillis, TimeUnit.MILLISECONDS)
                    .readTimeout(clientConfig.readTimeoutMillis, TimeUnit.MILLISECONDS)
                    .writeTimeout(clientConfig.writeTimeoutMillis, TimeUnit.MILLISECONDS)
                    .callTimeout(clientConfig.callTimeoutMillis, TimeUnit.MILLISECONDS)
                    .applyNetworkSecurityConfig(clientConfig.securityConfig)
                    .addInterceptor(RequestIdInterceptor())
                    .build()

            return Retrofit
                .Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(defaultNetworkJson().asConverterFactory(JSON_MEDIA_TYPE))
                .build()
                .create(RefreshApi::class.java)
        }

        private fun normalizeBaseUrl(baseUrl: String): String = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"

        private suspend fun executeRefresh(refreshToken: String): ApiResponse<SessionTokens> =
            networkCallExecutor.execute(
                call = {
                    refreshApi.refresh(
                        body = mapOf("refreshToken" to refreshToken),
                    )
                },
                parser =
                    mapParser { map ->
                        SessionTokens(
                            accessToken = map.requiredString("accessToken"),
                            refreshToken = map.requiredString("refreshToken"),
                        )
                    },
                throwOnError = false,
            )

        private suspend fun applyRefreshResponse(response: ApiResponse<SessionTokens>): Boolean {
            if (response.isError) {
                handleRefreshError(response = response)
                return false
            }

            val tokens = response.data
            if (tokens != null) {
                sessionManager.setTokens(tokens)
            }
            return tokens != null
        }

        private suspend fun handleRefreshError(response: ApiResponse<SessionTokens>) {
            if (response.statusCode == 401 || response.statusCode == 403) {
                sessionManager.logout()
            }
        }

        private fun Map<String, Any?>.requiredString(key: String): String =
            (get(key) as? String)?.takeIf { it.isNotBlank() }
                ?: error("Missing required field '$key' in refresh response.")

        private interface RefreshApi {
            @POST("v1/auth/refresh")
            suspend fun refresh(
                @Header(NetworkHeaders.REQUIRES_AUTH) requiresAuth: String = "false",
                @Body body: Map<String, String>,
            ): Response<ResponseBody>
        }

        private companion object {
            private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        }
    }
