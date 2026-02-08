package dev.fikril.androidcorekit.feature.auth.data.repository

import dev.fikril.androidcorekit.core.common.AppError
import dev.fikril.androidcorekit.core.common.AppResult
import dev.fikril.androidcorekit.core.network.error.NetworkLocalCode
import dev.fikril.androidcorekit.core.network.error.NetworkLocalStatus
import dev.fikril.androidcorekit.core.network.model.ApiResponse
import dev.fikril.androidcorekit.core.network.model.ApiStatus
import dev.fikril.androidcorekit.feature.auth.data.model.remote.LoginRequestDto
import dev.fikril.androidcorekit.feature.auth.data.model.remote.LoginResponseDto
import dev.fikril.androidcorekit.feature.auth.data.model.remote.LoginUserDto
import dev.fikril.androidcorekit.feature.auth.data.remote.AuthRemoteDataSource
import dev.fikril.androidcorekit.feature.auth.domain.model.LoginRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {
    @Test
    fun `maps successful response to auth session`() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    remoteDataSource =
                        FakeAuthRemoteDataSource(
                            nextResponse =
                                ApiResponse.success(
                                    data =
                                        LoginResponseDto(
                                            user = LoginUserDto(id = "user-1"),
                                            accessToken = "access-token",
                                            refreshToken = "refresh-token",
                                        ),
                                ),
                        ),
                )

            val result = repository.login(LoginRequest(email = "user@example.com", password = "secret"))

            assertTrue(result is AppResult.Success)
            val session = (result as AppResult.Success).value
            assertEquals("user-1", session.userId)
            assertEquals("access-token", session.accessToken)
            assertEquals("refresh-token", session.refreshToken)
        }

    @Test
    fun `maps null success data to parsing failure`() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    remoteDataSource =
                        FakeAuthRemoteDataSource(
                            nextResponse =
                                ApiResponse(
                                    status = ApiStatus.SUCCESS,
                                    data = null,
                                    message = null,
                                    code = null,
                                    traceId = null,
                                    meta = null,
                                    errors = null,
                                    statusCode = 200,
                                ),
                        ),
                )

            val result = repository.login(LoginRequest(email = "user@example.com", password = "secret"))

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Parsing)
        }

    @Test
    fun `maps network api error to app network error`() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    remoteDataSource =
                        FakeAuthRemoteDataSource(
                            nextResponse =
                                ApiResponse.error(
                                    message = "No internet connection.",
                                    statusCode = NetworkLocalStatus.NO_INTERNET,
                                    code = NetworkLocalCode.NO_INTERNET,
                                ),
                        ),
                )

            val result = repository.login(LoginRequest(email = "user@example.com", password = "secret"))

            assertTrue(result is AppResult.Failure)
            val error = (result as AppResult.Failure).error
            assertTrue(error is AppError.Network)
            assertEquals(NetworkLocalCode.NO_INTERNET, (error as AppError.Network).code)
        }
}

private class FakeAuthRemoteDataSource(
    var nextResponse: ApiResponse<LoginResponseDto>,
) : AuthRemoteDataSource {
    override suspend fun login(request: LoginRequestDto): ApiResponse<LoginResponseDto> = nextResponse
}
