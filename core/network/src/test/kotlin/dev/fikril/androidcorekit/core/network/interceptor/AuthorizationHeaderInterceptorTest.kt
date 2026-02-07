package dev.fikril.androidcorekit.core.network.interceptor

import dev.fikril.androidcorekit.core.network.auth.AccessTokenProvider
import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthorizationHeaderInterceptorTest {
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
    fun `adds bearer token by default when auth is required`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(AuthorizationHeaderInterceptor(AccessTokenProvider { "token-1" }))
                .build()

        client
            .newCall(
                Request
                    .Builder()
                    .url(server.url("/v1/me"))
                    .build(),
            ).execute()
            .use { }

        val recordedRequest = server.takeRequest()
        assertEquals("Bearer token-1", recordedRequest.getHeader(NetworkHeaders.AUTHORIZATION))
    }

    @Test
    fun `skips auth and strips internal requires auth header`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(AuthorizationHeaderInterceptor(AccessTokenProvider { "token-1" }))
                .build()

        client
            .newCall(
                Request
                    .Builder()
                    .url(server.url("/v1/auth/password/login"))
                    .header(NetworkHeaders.REQUIRES_AUTH, "false")
                    .build(),
            ).execute()
            .use { }

        val recordedRequest = server.takeRequest()
        assertNull(recordedRequest.getHeader(NetworkHeaders.AUTHORIZATION))
        assertNull(recordedRequest.getHeader(NetworkHeaders.REQUIRES_AUTH))
    }
}
