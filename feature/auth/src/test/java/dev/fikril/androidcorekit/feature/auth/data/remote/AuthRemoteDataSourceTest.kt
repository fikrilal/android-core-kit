package dev.fikril.androidcorekit.feature.auth.data.remote

import dev.fikril.androidcorekit.core.common.AppDispatchers
import dev.fikril.androidcorekit.core.network.client.NetworkClientFactory
import dev.fikril.androidcorekit.core.network.client.RetrofitServiceFactory
import dev.fikril.androidcorekit.core.network.client.StaticApiBaseUrlProvider
import dev.fikril.androidcorekit.core.network.execution.NetworkCallExecutor
import dev.fikril.androidcorekit.feature.auth.data.model.remote.LoginRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRemoteDataSourceTest {
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
    fun `login decodes response into typed dto and sends expected request body`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "data": {
                            "user": { "id": "user-1" },
                            "accessToken": "access-token",
                            "refreshToken": "refresh-token"
                          }
                        }
                        """.trimIndent(),
                    ),
            )

            val retrofitServiceFactory =
                RetrofitServiceFactory(
                    baseUrlProvider = StaticApiBaseUrlProvider(server.url("/").toString()),
                    networkClientFactory = NetworkClientFactory(),
                )
            val dataSource =
                DefaultAuthRemoteDataSource(
                    retrofitServiceFactory = retrofitServiceFactory,
                    networkCallExecutor = NetworkCallExecutor(dispatchers = TestDispatchers),
                )

            val response =
                dataSource.login(
                    LoginRequestDto(
                        email = "user@example.com",
                        password = "secret-123",
                        deviceId = "device-1",
                        deviceName = "Pixel",
                    ),
                )

            assertTrue(response.isSuccess)
            val payload = requireNotNull(response.data)
            assertEquals("user-1", payload.user.id)
            assertEquals("access-token", payload.accessToken)
            assertEquals("refresh-token", payload.refreshToken)

            val recorded = server.takeRequest()
            assertEquals("POST", recorded.method)
            assertEquals("/v1/auth/password/login", recorded.path)
            val body = recorded.body.readUtf8()
            assertTrue(body.contains("\"email\":\"user@example.com\""))
            assertTrue(body.contains("\"password\":\"secret-123\""))
            assertTrue(body.contains("\"deviceId\":\"device-1\""))
            assertTrue(body.contains("\"deviceName\":\"Pixel\""))
        }
}

private object TestDispatchers : AppDispatchers {
    override val default = Dispatchers.Unconfined
    override val io = Dispatchers.Unconfined
    override val main = Dispatchers.Unconfined
}
