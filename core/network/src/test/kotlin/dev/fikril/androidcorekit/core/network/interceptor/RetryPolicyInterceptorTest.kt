package dev.fikril.androidcorekit.core.network.interceptor

import dev.fikril.androidcorekit.core.network.client.NetworkRetryPolicy
import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import dev.fikril.androidcorekit.core.network.telemetry.NetworkOutcome
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

private typealias TelemetryEvents = CopyOnWriteArrayList<NetworkTelemetryEvent>

class RetryPolicyInterceptorTest {
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
    fun `retries get request on retryable status until success`() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(200))
        val events = TelemetryEvents()

        val client = createClient(events = events)
        val response =
            client
                .newCall(
                    Request
                        .Builder()
                        .url(server.url("/v1/me"))
                        .get()
                        .build(),
                ).execute()

        response.use {
            assertEquals(200, it.code)
        }
        assertEquals(2, server.requestCount)
        assertEquals(1, events.size)
        assertEquals(NetworkOutcome.SUCCESS, events.first().outcome)
        assertEquals(2, events.first().attemptCount)
    }

    @Test
    fun `does not retry post request without idempotency key`() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(200))

        val client = createClient()
        val response =
            client
                .newCall(
                    Request
                        .Builder()
                        .url(server.url("/v1/auth/password/login"))
                        .post("{}".toRequestBody())
                        .build(),
                ).execute()

        response.use {
            assertEquals(503, it.code)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `retries post request when idempotency key exists`() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(200))

        val client = createClient()
        val response =
            client
                .newCall(
                    Request
                        .Builder()
                        .url(server.url("/v1/auth/password/login"))
                        .header(NetworkHeaders.IDEMPOTENCY_KEY, "request-1")
                        .post("{}".toRequestBody())
                        .build(),
                ).execute()

        response.use {
            assertEquals(200, it.code)
        }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `retries transport io exception for retryable request`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val attempts = AtomicInteger(0)

        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(
                    RetryPolicyInterceptor(
                        policy = noDelayRetryPolicy(),
                        sleeper = {},
                    ),
                ).addInterceptor { chain ->
                    val attempt = attempts.incrementAndGet()
                    if (attempt == 1) {
                        throw IOException("simulated transport failure")
                    }
                    chain.proceed(chain.request())
                }.build()

        val response =
            client
                .newCall(
                    Request
                        .Builder()
                        .url(server.url("/v1/me"))
                        .get()
                        .build(),
                ).execute()

        response.use {
            assertEquals(200, it.code)
        }
        assertEquals(2, attempts.get())
    }

    @Test
    fun `emits network error telemetry when retries exhausted`() {
        val events = TelemetryEvents()
        val attempts = AtomicInteger(0)
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(
                    RetryPolicyInterceptor(
                        policy = noDelayRetryPolicy(maxAttempts = 2),
                        telemetryObserver = { event -> events.add(event) },
                        sleeper = {},
                    ),
                ).addInterceptor {
                    attempts.incrementAndGet()
                    throw IOException("always failing")
                }.build()

        runCatching {
            client
                .newCall(
                    Request
                        .Builder()
                        .url(server.url("/v1/me"))
                        .build(),
                ).execute()
        }

        assertEquals(2, attempts.get())
        assertEquals(1, events.size)
        assertEquals(NetworkOutcome.NETWORK_ERROR, events.first().outcome)
        assertEquals(2, events.first().attemptCount)
        assertTrue(events.first().durationMillis >= 0)
    }

    private fun createClient(events: TelemetryEvents = TelemetryEvents()): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(
                RetryPolicyInterceptor(
                    policy = noDelayRetryPolicy(),
                    telemetryObserver = { event -> events.add(event) },
                    sleeper = {},
                ),
            ).build()

    private fun noDelayRetryPolicy(maxAttempts: Int = 3): NetworkRetryPolicy =
        NetworkRetryPolicy(
            maxAttempts = maxAttempts,
            initialBackoffMillis = 0L,
            maxBackoffMillis = 0L,
        )
}
