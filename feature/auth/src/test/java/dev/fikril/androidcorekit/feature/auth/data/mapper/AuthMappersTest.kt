package dev.fikril.androidcorekit.feature.auth.data.mapper

import dev.fikril.androidcorekit.feature.auth.data.model.remote.LoginResponseDto
import dev.fikril.androidcorekit.feature.auth.data.model.remote.LoginUserDto
import dev.fikril.androidcorekit.feature.auth.domain.model.LoginRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthMappersTest {
    @Test
    fun `toDto maps login request fields`() {
        val request =
            LoginRequest(
                email = "user@example.com",
                password = "secret-123",
                deviceId = "device-1",
                deviceName = "Pixel",
            )

        val dto = request.toDto()

        assertEquals("user@example.com", dto.email)
        assertEquals("secret-123", dto.password)
        assertEquals("device-1", dto.deviceId)
        assertEquals("Pixel", dto.deviceName)
    }

    @Test
    fun `toDomain maps response dto fields`() {
        val dto =
            LoginResponseDto(
                user = LoginUserDto(id = "user-1"),
                accessToken = "access-token",
                refreshToken = "refresh-token",
            )

        val domain = dto.toDomain()

        assertEquals("user-1", domain.userId)
        assertEquals("access-token", domain.accessToken)
        assertEquals("refresh-token", domain.refreshToken)
    }
}
