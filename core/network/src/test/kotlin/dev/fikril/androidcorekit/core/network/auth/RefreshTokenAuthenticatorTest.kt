package dev.fikril.androidcorekit.core.network.auth

import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

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
                response = unauthorizedResponse(authorization = "Bearer old-token"),
            )

        assertNotNull(updated)
        assertEquals(1, refreshCalls)
        assertEquals("Bearer new-token", updated?.header(NetworkHeaders.AUTHORIZATION))
    }

    @Test
    fun `does not retry post without idempotency key`() {
        var refreshCalls = 0
        val authenticator =
            RefreshTokenAuthenticator(
                accessTokenProvider = AccessTokenProvider { "old-token" },
                accessTokenRefresher =
                    AccessTokenRefresher {
                        refreshCalls += 1
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
                    ),
            )

        assertNull(updated)
        assertEquals(0, refreshCalls)
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

    private fun unauthorizedResponse(
        method: String = "GET",
        authorization: String? = null,
        idempotencyKey: String? = null,
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
