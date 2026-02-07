package dev.fikril.androidcorekit.core.network

import dev.fikril.androidcorekit.core.common.AppDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApiHelperTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `post parses success envelope with parser adapter`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("X-Request-Id", "req-1")
                    .setBody(
                        """
                        {
                          "data": {
                            "accessToken": "access-1",
                            "refreshToken": "refresh-1"
                          },
                          "meta": {
                            "source": "auth"
                          }
                        }
                        """.trimIndent(),
                    ),
            )

            val helper = createHelper()
            val response =
                helper.post(
                    path = "/v1/auth/password/login",
                    parser =
                        mapParser { map ->
                            AuthPayload(
                                accessToken = map["accessToken"] as String,
                                refreshToken = map["refreshToken"] as String,
                            )
                        },
                    host = ApiHost.AUTH,
                    data = mapOf("email" to "hello@example.com", "password" to "Secret123"),
                    requiresAuth = false,
                    throwOnError = false,
                )

            assertTrue(response.isSuccess)
            assertEquals("access-1", response.data?.accessToken)
            assertEquals("refresh-1", response.data?.refreshToken)
            assertEquals("req-1", response.traceId)
            assertEquals(
                "auth",
                response.meta
                    ?.get("source")
                    ?.toString()
                    ?.removeSurrounding("\""),
            )
        }

    @Test
    fun `post returns error response when throwOnError is false`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setHeader("X-Request-Id", "req-401")
                    .setBody(
                        """
                        {
                          "title": "Invalid credentials",
                          "status": 401,
                          "code": "UNAUTHORIZED",
                          "traceId": "trace-401"
                        }
                        """.trimIndent(),
                    ),
            )

            val helper = createHelper()
            val response =
                helper.post(
                    path = "/v1/auth/password/login",
                    parser = noDataParser,
                    host = ApiHost.AUTH,
                    data = mapOf("email" to "hello@example.com", "password" to "BadSecret"),
                    requiresAuth = false,
                    throwOnError = false,
                )

            assertTrue(response.isError)
            assertEquals(401, response.statusCode)
            assertEquals("UNAUTHORIZED", response.code)
            assertEquals("Invalid credentials", response.message)
            assertEquals("trace-401", response.traceId)
        }

    @Test
    fun `post throws ApiException when throwOnError is true`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody(
                        """
                        {
                          "title": "Invalid credentials",
                          "status": 401,
                          "code": "UNAUTHORIZED"
                        }
                        """.trimIndent(),
                    ),
            )

            val helper = createHelper()

            try {
                helper.post(
                    path = "/v1/auth/password/login",
                    parser = noDataParser,
                    host = ApiHost.AUTH,
                    data = mapOf("email" to "hello@example.com", "password" to "BadSecret"),
                    requiresAuth = false,
                    throwOnError = true,
                )
                fail("Expected ApiException")
            } catch (exception: ApiException) {
                assertEquals(401, exception.statusCode)
                assertEquals("UNAUTHORIZED", exception.code)
                assertNotNull(exception.message)
            }
        }

    @Test
    fun `get retries once on 401 after successful token refresh`() =
        runTest {
            var token = "old-token"
            var refreshCalls = 0

            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"data": {"ok": true}}"""),
            )

            val helper =
                createHelper(
                    accessTokenProvider = AccessTokenProvider { token },
                    accessTokenRefresher =
                        AccessTokenRefresher {
                            refreshCalls += 1
                            token = "new-token"
                            true
                        },
                )

            val response =
                helper.get(
                    path = "/v1/me",
                    parser = noDataParser,
                    requiresAuth = true,
                    throwOnError = false,
                )

            assertTrue(response.isSuccess)
            assertEquals(1, refreshCalls)
            assertEquals(2, server.requestCount)
            assertEquals("Bearer old-token", server.takeRequest().getHeader("Authorization"))
            assertEquals("Bearer new-token", server.takeRequest().getHeader("Authorization"))
        }

    @Test
    fun `post does not retry on 401 without idempotency key`() =
        runTest {
            var refreshCalls = 0

            server.enqueue(MockResponse().setResponseCode(401))

            val helper =
                createHelper(
                    accessTokenProvider = AccessTokenProvider { "access-token" },
                    accessTokenRefresher =
                        AccessTokenRefresher {
                            refreshCalls += 1
                            true
                        },
                )

            val response =
                helper.post(
                    path = "/v1/me/profile",
                    parser = noDataParser,
                    data = mapOf("name" to "Alice"),
                    requiresAuth = true,
                    throwOnError = false,
                )

            assertTrue(response.isError)
            assertEquals(0, refreshCalls)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `post retries on 401 when idempotency key is present`() =
        runTest {
            var token = "old-token"
            var refreshCalls = 0

            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"data": {"ok": true}}"""),
            )

            val helper =
                createHelper(
                    accessTokenProvider = AccessTokenProvider { token },
                    accessTokenRefresher =
                        AccessTokenRefresher {
                            refreshCalls += 1
                            token = "new-token"
                            true
                        },
                )

            val response =
                helper.post(
                    path = "/v1/me/profile",
                    parser = noDataParser,
                    data = mapOf("name" to "Alice"),
                    headers = mapOf("Idempotency-Key" to "req-1"),
                    requiresAuth = true,
                    throwOnError = false,
                )

            assertTrue(response.isSuccess)
            assertEquals(1, refreshCalls)
            assertEquals(2, server.requestCount)
            assertEquals("Bearer old-token", server.takeRequest().getHeader("Authorization"))
            assertEquals("Bearer new-token", server.takeRequest().getHeader("Authorization"))
        }

    private fun createHelper(
        accessTokenProvider: AccessTokenProvider = AccessTokenProvider { null },
        accessTokenRefresher: AccessTokenRefresher? = null,
    ): ApiHelper =
        ApiHelper(
            client = OkHttpClient(),
            baseUrlProvider =
                ApiBaseUrlProvider {
                    server.url("/").toString()
                },
            dispatchers = TestDispatchers,
            accessTokenProvider = accessTokenProvider,
            accessTokenRefresher = accessTokenRefresher,
        )

    private object TestDispatchers : AppDispatchers {
        private val dispatcher = UnconfinedTestDispatcher()
        override val default: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private data class AuthPayload(
        val accessToken: String,
        val refreshToken: String,
    )
}
