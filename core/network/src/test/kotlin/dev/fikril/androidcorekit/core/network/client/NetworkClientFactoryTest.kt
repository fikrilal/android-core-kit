package dev.fikril.androidcorekit.core.network.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NetworkClientFactoryTest {
    @Test
    fun `applies configured timeout and connection defaults`() {
        val config =
            NetworkClientConfig(
                connectTimeoutMillis = 11_000L,
                readTimeoutMillis = 22_000L,
                writeTimeoutMillis = 33_000L,
                callTimeoutMillis = 44_000L,
                maxIdleConnections = 7,
                keepAliveDurationMinutes = 3L,
            )

        val client = NetworkClientFactory(clientConfig = config).create()

        assertEquals(11_000, client.connectTimeoutMillis)
        assertEquals(22_000, client.readTimeoutMillis)
        assertEquals(33_000, client.writeTimeoutMillis)
        assertEquals(44_000, client.callTimeoutMillis)
        assertFalse(client.retryOnConnectionFailure)
    }
}
