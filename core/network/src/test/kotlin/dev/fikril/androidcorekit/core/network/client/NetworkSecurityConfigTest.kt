package dev.fikril.androidcorekit.core.network.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSecurityConfigTest {
    @Test(expected = IllegalArgumentException::class)
    fun `enforced pinning requires at least one host`() {
        NetworkSecurityConfig(
            pinningMode = NetworkPinningMode.ENFORCED,
            pinnedCertificatesByHost = emptyMap(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects hosts that include scheme`() {
        NetworkSecurityConfig(
            pinningMode = NetworkPinningMode.ENFORCED,
            pinnedCertificatesByHost =
                mapOf(
                    "https://api.example.com" to
                        setOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="),
                ),
        )
    }

    @Test
    fun `normalizes blank pins out of map`() {
        val normalized =
            NetworkSecurityConfig.normalizePins(
                mapOf(
                    "api.example.com" to
                        setOf(
                            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                            "",
                        ),
                ),
            )

        assertEquals(1, normalized.size)
        assertTrue(normalized["api.example.com"]?.size == 1)
    }
}
