package dev.fikril.androidcorekit.feature.auth.data.remote

import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import dev.fikril.androidcorekit.feature.auth.data.model.remote.LoginRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface AuthApi {
    @POST("v1/auth/password/login")
    suspend fun login(
        @Header(NetworkHeaders.REQUIRES_AUTH) requiresAuth: String = "false",
        @Body body: LoginRequestDto,
    ): Response<ResponseBody>
}
