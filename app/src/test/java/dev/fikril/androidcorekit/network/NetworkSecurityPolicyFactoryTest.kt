package dev.fikril.androidcorekit.network

import dev.fikril.androidcorekit.core.network.client.NetworkPinningMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSecurityPolicyFactoryTest {
    private val factory = NetworkSecurityPolicyFactory()

    @Test
    fun `returns relaxed policy for dev environment`() {
        val policy =
            factory.create(
                baseUrl = "https://dev.example.invalid",
                isProd = false,
                primaryPin = "",
                backupPin = "",
            )

        assertEquals(NetworkPinningMode.RELAXED, policy.pinningMode)
        assertTrue(policy.pinnedCertificatesByHost.isEmpty())
        assertFalse(policy.enforceModernTls)
    }

    @Test
    fun `returns enforced policy for prod environment`() {
        val policy =
            factory.create(
                baseUrl = "https://prod.example.invalid",
                isProd = true,
                primaryPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                backupPin = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=",
            )

        assertEquals(NetworkPinningMode.ENFORCED, policy.pinningMode)
        assertEquals(setOf("prod.example.invalid"), policy.pinnedCertificatesByHost.keys)
        assertTrue(policy.enforceModernTls)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for prod when backup pin is missing`() {
        factory.create(
            baseUrl = "https://prod.example.invalid",
            isProd = true,
            primaryPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            backupPin = "",
        )
    }
}
