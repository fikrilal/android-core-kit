package dev.fikril.androidcorekit.core.network.execution

import dev.fikril.androidcorekit.core.common.AppDispatchers
import dev.fikril.androidcorekit.core.network.error.ApiException
import dev.fikril.androidcorekit.core.network.error.NetworkLocalCode
import dev.fikril.androidcorekit.core.network.error.NetworkLocalStatus
import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import dev.fikril.androidcorekit.core.network.serialization.mapParser
import dev.fikril.androidcorekit.core.network.serialization.noDataParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkCallExecutorTest {
    private val executor = NetworkCallExecutor(dispatchers = TestDispatchers)

    @Test
    fun `execute parses success envelope`() =
        runTest {
            val response =
                executor.execute(
                    call = {
                        Response.success(
                            """
                            {
                              "data": {
                                "accessToken": "token-1",
                                "refreshToken": "refresh-1"
                              },
                              "meta": {
                                "source": "auth"
                              }
                            }
                            """.trimIndent().toResponseBody(JSON_MEDIA_TYPE),
                            Headers.headersOf(NetworkHeaders.REQUEST_ID, "req-1"),
                        )
                    },
                    parser =
                        mapParser { map ->
                            TestPayload(
                                accessToken = map["accessToken"] as String,
                                refreshToken = map["refreshToken"] as String,
                            )
                        },
                    throwOnError = false,
                )

            assertTrue(response.isSuccess)
            assertEquals("token-1", response.data?.accessToken)
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
    fun `execute maps error response body`() =
        runTest {
            val request =
                Request
                    .Builder()
                    .url("https://example.com/v1/auth/password/login")
                    .build()
            val rawResponse =
                okhttp3.Response
                    .Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .header(NetworkHeaders.REQUEST_ID, "req-401")
                    .build()

            val response =
                executor.execute(
                    call = {
                        Response.error(
                            """
                            {
                              "title": "Invalid credentials",
                              "status": 401,
                              "code": "UNAUTHORIZED",
                              "traceId": "trace-401"
                            }
                            """.trimIndent().toResponseBody(JSON_MEDIA_TYPE),
                            rawResponse,
                        )
                    },
                    parser = noDataParser,
                    throwOnError = false,
                )

            assertTrue(response.isError)
            assertEquals(401, response.statusCode)
            assertEquals("UNAUTHORIZED", response.code)
            assertEquals("Invalid credentials", response.message)
            assertEquals("trace-401", response.traceId)
        }

    @Test
    fun `execute maps transport failures and throws ApiException when requested`() =
        runTest {
            try {
                executor.execute(
                    call = { throw UnknownHostException("offline") },
                    parser = noDataParser,
                    throwOnError = true,
                )
                fail("Expected ApiException")
            } catch (exception: ApiException) {
                assertEquals(NetworkLocalStatus.NO_INTERNET, exception.statusCode)
                assertEquals(NetworkLocalCode.NO_INTERNET, exception.code)
                assertEquals("No internet connection.", exception.message)
            }
        }

    private object TestDispatchers : AppDispatchers {
        private val dispatcher = UnconfinedTestDispatcher()
        override val default: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private data class TestPayload(
        val accessToken: String,
        val refreshToken: String,
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
