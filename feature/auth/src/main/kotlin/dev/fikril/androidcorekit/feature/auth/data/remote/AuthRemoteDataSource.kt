package dev.fikril.androidcorekit.feature.auth.data.remote

import dev.fikril.androidcorekit.core.network.client.RetrofitServiceFactory
import dev.fikril.androidcorekit.core.network.execution.NetworkCallExecutor
import dev.fikril.androidcorekit.core.network.model.ApiHost
import dev.fikril.androidcorekit.core.network.model.ApiResponse
import dev.fikril.androidcorekit.core.network.serialization.defaultNetworkJson
import dev.fikril.androidcorekit.feature.auth.data.model.remote.LoginRequestDto
import dev.fikril.androidcorekit.feature.auth.data.model.remote.LoginResponseDto
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRemoteDataSource {
    suspend fun login(request: LoginRequestDto): ApiResponse<LoginResponseDto>
}

@Singleton
class DefaultAuthRemoteDataSource
    @Inject
    constructor(
        private val retrofitServiceFactory: RetrofitServiceFactory,
        private val networkCallExecutor: NetworkCallExecutor,
    ) : AuthRemoteDataSource {
        private val authApi: AuthApi by lazy { retrofitServiceFactory.create<AuthApi>(ApiHost.AUTH) }
        private val json = defaultNetworkJson()

        override suspend fun login(request: LoginRequestDto): ApiResponse<LoginResponseDto> =
            networkCallExecutor.execute(
                call = { authApi.login(body = request) },
                parser = { payload -> json.decodeFromJsonElement(payload) },
                throwOnError = false,
            )
    }
