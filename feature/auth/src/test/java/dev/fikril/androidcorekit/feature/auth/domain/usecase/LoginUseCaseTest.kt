package dev.fikril.androidcorekit.feature.auth.domain.usecase

import dev.fikril.androidcorekit.core.common.AppError
import dev.fikril.androidcorekit.core.common.AppResult
import dev.fikril.androidcorekit.core.session.SessionManager
import dev.fikril.androidcorekit.core.session.SessionState
import dev.fikril.androidcorekit.core.session.SessionTokens
import dev.fikril.androidcorekit.feature.auth.domain.model.AuthSession
import dev.fikril.androidcorekit.feature.auth.domain.model.LoginRequest
import dev.fikril.androidcorekit.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {
    @Test
    fun `returns validation failure when email or password invalid`() =
        runTest {
            val repository = FakeAuthRepository()
            val sessionManager = FakeSessionManager()
            val useCase = LoginUseCase(authRepository = repository, sessionManager = sessionManager)

            val result = useCase(email = "invalid-email", password = "")

            assertTrue(result is AppResult.Failure)
            val error = (result as AppResult.Failure).error as AppError.Validation
            assertTrue(error.fieldErrors.containsKey("email"))
            assertTrue(error.fieldErrors.containsKey("password"))
            assertEquals(0, repository.loginCalls)
        }

    @Test
    fun `persists session when login succeeds`() =
        runTest {
            val repository =
                FakeAuthRepository(
                    nextResult =
                        AppResult.Success(
                            AuthSession(
                                userId = "user-123",
                                accessToken = "access-token",
                                refreshToken = "refresh-token",
                            ),
                        ),
                )
            val sessionManager = FakeSessionManager()
            val useCase = LoginUseCase(authRepository = repository, sessionManager = sessionManager)

            val result = useCase(email = "user@example.com", password = "super-secret")

            assertTrue(result is AppResult.Success)
            assertEquals(1, repository.loginCalls)
            assertEquals("access-token", sessionManager.accessToken())
            assertEquals("refresh-token", sessionManager.refreshToken())
            assertEquals("user-123", sessionManager.authenticatedUserId)
        }

    @Test
    fun `does not mutate session when repository fails`() =
        runTest {
            val repository = FakeAuthRepository(nextResult = AppResult.Failure(AppError.Unauthorized))
            val sessionManager = FakeSessionManager()
            val useCase = LoginUseCase(authRepository = repository, sessionManager = sessionManager)

            val result = useCase(email = "user@example.com", password = "super-secret")

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Unauthorized)
            assertEquals(1, repository.loginCalls)
            assertNull(sessionManager.accessToken())
            assertNull(sessionManager.refreshToken())
            assertNull(sessionManager.authenticatedUserId)
        }
}

private class FakeAuthRepository(
    var nextResult: AppResult<AuthSession> = AppResult.Failure(AppError.Unknown()),
) : AuthRepository {
    var loginCalls: Int = 0

    override suspend fun login(request: LoginRequest): AppResult<AuthSession> {
        loginCalls += 1
        return nextResult
    }
}

private class FakeSessionManager : SessionManager {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    private var tokens: SessionTokens? = null
    var authenticatedUserId: String? = null

    override val state: StateFlow<SessionState> = mutableState

    override fun accessToken(): String? = tokens?.accessToken

    override fun refreshToken(): String? = tokens?.refreshToken

    override suspend fun setTokens(tokens: SessionTokens) {
        this.tokens = tokens
    }

    override suspend fun setAuthenticated(userId: String?) {
        authenticatedUserId = userId
        mutableState.value = SessionState.Authenticated(userId = userId)
    }

    override suspend fun setUnauthenticated() {
        tokens = null
        authenticatedUserId = null
        mutableState.value = SessionState.Unauthenticated
    }

    override suspend fun clearTokens() {
        tokens = null
    }
}
