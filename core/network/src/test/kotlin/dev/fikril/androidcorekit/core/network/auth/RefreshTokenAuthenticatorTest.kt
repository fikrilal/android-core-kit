package dev.fikril.androidcorekit.core.network.auth

import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import dev.fikril.androidcorekit.core.network.telemetry.NetworkRefreshOutcome
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryEvent
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryEventType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

private typealias TelemetryEvents = CopyOnWriteArrayList<NetworkTelemetryEvent>

class RefreshTokenAuthenticatorTest {
    @Test
    fun `returns null when original request has no authorization`() {
        val authenticator = RefreshTokenAuthenticator()

        val updated = authenticator.authenticate(route = null, response = unauthorizedResponse())

        assertNull(updated)
    }

    @Test
    fun `retries get request with refreshed token`() {
        var token = "old-token"
        var refreshCalls = 0
        val events = TelemetryEvents()
        val authenticator =
            RefreshTokenAuthenticator(
                accessTokenProvider = AccessTokenProvider { token },
                accessTokenRefresher =
                    AccessTokenRefresher {
                        refreshCalls += 1
                        token = "new-token"
                        true
                    },
                telemetryObserver = { event -> events.add(event) },
            )

        val updated =
            authenticator.authenticate(
                route = null,
                response =
                    unauthorizedResponse(
                        authorization = "Bearer old-token",
                        requestId = "req-auth-1",
                    ),
            )

        assertNotNull(updated)
        assertEquals(1, refreshCalls)
        assertEquals("Bearer new-token", updated?.header(NetworkHeaders.AUTHORIZATION))
        assertEquals(2, events.size)
        assertEquals(NetworkTelemetryEventType.REFRESH_ATTEMPT, events[0].eventType)
        assertEquals(NetworkTelemetryEventType.REFRESH_RESULT, events[1].eventType)
        assertEquals(NetworkRefreshOutcome.SUCCESS, events[1].refreshOutcome)
        assertEquals("req-auth-1", events[1].requestId)
    }

    @Test
    fun `does not retry post without idempotency key`() {
        var refreshCalls = 0
        val events = TelemetryEvents()
        val authenticator =
            RefreshTokenAuthenticator(
                accessTokenProvider = AccessTokenProvider { "old-token" },
                accessTokenRefresher =
                    AccessTokenRefresher {
                        refreshCalls += 1
                        true
                    },
                telemetryObserver = { event -> events.add(event) },
            )

        val updated =
            authenticator.authenticate(
                route = null,
                response =
                    unauthorizedResponse(
                        method = "POST",
                        authorization = "Bearer old-token",
                    ),
            )

        assertNull(updated)
        assertEquals(0, refreshCalls)
        assertEquals(1, events.size)
        assertEquals(NetworkTelemetryEventType.REFRESH_RESULT, events[0].eventType)
        assertEquals(NetworkRefreshOutcome.SKIPPED, events[0].refreshOutcome)
    }

    @Test
    fun `retries post when idempotency key is present`() {
        var token = "old-token"
        var refreshCalls = 0
        val authenticator =
            RefreshTokenAuthenticator(
                accessTokenProvider = AccessTokenProvider { token },
                accessTokenRefresher =
                    AccessTokenRefresher {
                        refreshCalls += 1
                        token = "new-token"
                        true
                    },
            )

        val updated =
            authenticator.authenticate(
                route = null,
                response =
                    unauthorizedResponse(
                        method = "POST",
                        authorization = "Bearer old-token",
                        idempotencyKey = "req-1",
                    ),
            )

        assertNotNull(updated)
        assertEquals(1, refreshCalls)
        assertEquals("Bearer new-token", updated?.header(NetworkHeaders.AUTHORIZATION))
    }

    @Test
    fun `emits redacted telemetry message when refresh throws`() {
        val events = TelemetryEvents()
        val authenticator =
            RefreshTokenAuthenticator(
                accessTokenProvider = AccessTokenProvider { "old-token" },
                accessTokenRefresher =
                    AccessTokenRefresher {
                        error("accessToken=plain-secret for john.doe@example.com")
                    },
                telemetryObserver = { event -> events.add(event) },
            )

        val updated =
            authenticator.authenticate(
                route = null,
                response = unauthorizedResponse(authorization = "Bearer old-token"),
            )

        assertNull(updated)
        assertEquals(2, events.size)
        assertEquals(NetworkTelemetryEventType.REFRESH_RESULT, events[1].eventType)
        assertEquals(NetworkRefreshOutcome.FAILURE, events[1].refreshOutcome)
        assertEquals("REFRESH_EXCEPTION", events[1].errorCode)
        assertTrue((events[1].errorMessage ?: "").contains("[REDACTED]"))
        assertTrue((events[1].errorMessage ?: "").contains("[REDACTED_EMAIL]"))
    }

    @Test
    fun `refresh calls are single flight for concurrent unauthorized requests`() {
        var token = "old-token"
        val refreshCalls = AtomicInteger(0)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(4)
        val authenticator =
            RefreshTokenAuthenticator(
                accessTokenProvider = AccessTokenProvider { token },
                accessTokenRefresher =
                    AccessTokenRefresher {
                        startLatch.await(1, TimeUnit.SECONDS)
                        Thread.sleep(20L)
                        refreshCalls.incrementAndGet()
                        token = "new-token"
                        true
                    },
            )

        repeat(4) {
            thread {
                authenticator.authenticate(
                    route = null,
                    response = unauthorizedResponse(authorization = "Bearer old-token"),
                )
                doneLatch.countDown()
            }
        }

        startLatch.countDown()
        doneLatch.await(2, TimeUnit.SECONDS)

        assertEquals(1, refreshCalls.get())
    }

    private fun unauthorizedResponse(
        method: String = "GET",
        authorization: String? = null,
        idempotencyKey: String? = null,
        requestId: String? = null,
    ): Response {
        val requestBuilder =
            Request
                .Builder()
                .url("https://example.com/v1/me")
                .method(method, if (method in BODY_METHODS) "{}".toRequestBody() else null)

        if (!authorization.isNullOrBlank()) {
            requestBuilder.header(NetworkHeaders.AUTHORIZATION, authorization)
        }
        if (!idempotencyKey.isNullOrBlank()) {
            requestBuilder.header(NetworkHeaders.IDEMPOTENCY_KEY, idempotencyKey)
        }
        if (!requestId.isNullOrBlank()) {
            requestBuilder.header(NetworkHeaders.REQUEST_ID, requestId)
        }

        return Response
            .Builder()
            .request(requestBuilder.build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody())
            .build()
    }

    companion object {
        private val BODY_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}
