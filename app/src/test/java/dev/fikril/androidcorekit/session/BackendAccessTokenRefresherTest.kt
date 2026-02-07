package dev.fikril.androidcorekit.session

import dev.fikril.androidcorekit.core.common.AppDispatchers
import dev.fikril.androidcorekit.core.network.client.StaticApiBaseUrlProvider
import dev.fikril.androidcorekit.core.network.execution.NetworkCallExecutor
import dev.fikril.androidcorekit.core.session.SessionManager
import dev.fikril.androidcorekit.core.session.SessionState
import dev.fikril.androidcorekit.core.session.SessionTokens
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackendAccessTokenRefresherTest {
    private lateinit var server: MockWebServer
    private lateinit var sessionManager: TestSessionManager
    private lateinit var refresher: BackendAccessTokenRefresher

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        sessionManager = TestSessionManager()
        refresher =
            BackendAccessTokenRefresher(
                sessionManager = sessionManager,
                baseUrlProvider = StaticApiBaseUrlProvider(server.url("/").toString()),
                networkCallExecutor = NetworkCallExecutor(dispatchers = TestDispatchers),
            )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `refresh updates stored tokens on success`() =
        runTest {
            sessionManager.setTokens(
                SessionTokens(
                    accessToken = "access-old",
                    refreshToken = "refresh-old",
                ),
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "data": {
                            "user": {
                              "id": "user-1",
                              "email": "user@example.com",
                              "emailVerified": true
                            },
                            "accessToken": "access-new",
                            "refreshToken": "refresh-new"
                          }
                        }
                        """.trimIndent(),
                    ),
            )

            val refreshed = refresher.refreshAccessToken()

            assertTrue(refreshed)
            assertEquals("access-new", sessionManager.accessToken())
            assertEquals("refresh-new", sessionManager.refreshToken())

            val request = server.takeRequest()
            assertEquals("/v1/auth/refresh", request.path)
            assertNotNull(request.getHeader("X-Request-Id"))
            assertEquals("{\"refreshToken\":\"refresh-old\"}", request.body.readUtf8())
        }

    @Test
    fun `refresh clears session on unauthenticated response`() =
        runTest {
            sessionManager.setTokens(
                SessionTokens(
                    accessToken = "access-old",
                    refreshToken = "refresh-old",
                ),
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody(
                        """
                        {
                          "title": "Refresh token invalid",
                          "status": 401,
                          "code": "AUTH_REFRESH_TOKEN_INVALID"
                        }
                        """.trimIndent(),
                    ),
            )

            val refreshed = refresher.refreshAccessToken()

            assertFalse(refreshed)
            assertEquals(SessionState.Unauthenticated, sessionManager.state.value)
            assertEquals(null, sessionManager.accessToken())
            assertEquals(null, sessionManager.refreshToken())
        }

    @Test
    fun `refresh clears session on forbidden response`() =
        runTest {
            sessionManager.setTokens(
                SessionTokens(
                    accessToken = "access-old",
                    refreshToken = "refresh-old",
                ),
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(403)
                    .setBody(
                        """
                        {
                          "title": "Forbidden",
                          "status": 403,
                          "code": "AUTH_REFRESH_TOKEN_FORBIDDEN"
                        }
                        """.trimIndent(),
                    ),
            )

            val refreshed = refresher.refreshAccessToken()

            assertFalse(refreshed)
            assertEquals(SessionState.Unauthenticated, sessionManager.state.value)
            assertEquals(null, sessionManager.accessToken())
            assertEquals(null, sessionManager.refreshToken())
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private object TestDispatchers : AppDispatchers {
    private val dispatcher = UnconfinedTestDispatcher()

    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
}

private class TestSessionManager : SessionManager {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    private var tokens: SessionTokens? = null

    override val state: StateFlow<SessionState> = mutableState.asStateFlow()

    override fun accessToken(): String? = tokens?.accessToken

    override fun refreshToken(): String? = tokens?.refreshToken

    override suspend fun setTokens(tokens: SessionTokens) {
        this.tokens = tokens
        val userId = (mutableState.value as? SessionState.Authenticated)?.userId
        mutableState.value = SessionState.Authenticated(userId = userId)
    }

    override suspend fun setAuthenticated(userId: String?) {
        mutableState.value = SessionState.Authenticated(userId = userId)
    }

    override suspend fun setUnauthenticated() {
        tokens = null
        mutableState.value = SessionState.Unauthenticated
    }

    override suspend fun clearTokens() {
        tokens = null
    }
}
