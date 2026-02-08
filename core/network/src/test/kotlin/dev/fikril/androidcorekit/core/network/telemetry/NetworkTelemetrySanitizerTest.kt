package dev.fikril.androidcorekit.core.network.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkTelemetrySanitizerTest {
    @Test
    fun `sanitizeRoute keeps host and path only`() {
        val sanitized =
            NetworkTelemetrySanitizer.sanitizeRoute(
                "https://api.example.com:8443/v1/auth/login?email=john@example.com&token=abc#section",
            )

        assertEquals("api.example.com:8443/v1/auth/login", sanitized)
    }

    @Test
    fun `sanitizeText redacts token and email content`() {
        val raw =
            """
            Bearer eyJhbGciOiJIUzI1NiJ9.abc.def
            user=john.doe@example.com
            {"accessToken":"secret-value","refreshToken":"other-secret"}
            accessToken=plain-secret
            """.trimIndent()

        val sanitized = NetworkTelemetrySanitizer.sanitizeText(raw)

        assertTrue(sanitized?.contains("[REDACTED]") == true)
        assertTrue(sanitized?.contains("[REDACTED_EMAIL]") == true)
        assertFalse(sanitized?.contains("john.doe@example.com") == true)
        assertFalse(sanitized?.contains("secret-value") == true)
        assertFalse(sanitized?.contains("other-secret") == true)
        assertFalse(sanitized?.contains("plain-secret") == true)
    }
}
