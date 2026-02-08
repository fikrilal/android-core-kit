package dev.fikril.androidcorekit.core.network.error

import dev.fikril.androidcorekit.core.common.AppError
import dev.fikril.androidcorekit.core.network.model.ApiResponse
import dev.fikril.androidcorekit.core.network.model.ApiValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class ApiErrorMapperTest {
    @Test
    fun `toAppError maps unauthorized and forbidden to unauthorized`() {
        val unauthorizedError =
            ApiResponse.error<Unit>(
                message = "Unauthorized",
                statusCode = 401,
                code = "AUTH_UNAUTHORIZED",
            )
        val forbiddenError =
            ApiResponse.error<Unit>(
                message = "Forbidden",
                statusCode = 403,
                code = "AUTH_FORBIDDEN",
            )

        assertTrue(unauthorizedError.toAppError() is AppError.Unauthorized)
        assertTrue(forbiddenError.toAppError() is AppError.Unauthorized)
    }

    @Test
    fun `toAppError maps validation payload`() {
        val response =
            ApiResponse.error<Unit>(
                message = "Invalid payload",
                statusCode = 422,
                code = "VALIDATION_ERROR",
                errors =
                    listOf(
                        ApiValidationError(
                            field = "email",
                            message = "Email is invalid",
                        ),
                        ApiValidationError(
                            field = null,
                            message = "Payload malformed",
                        ),
                    ),
            )

        val appError = response.toAppError()

        assertTrue(appError is AppError.Validation)
        val validation = appError as AppError.Validation
        assertEquals("Invalid payload", validation.message)
        assertEquals(listOf("Email is invalid"), validation.fieldErrors["email"])
        assertEquals(listOf("Payload malformed"), validation.fieldErrors["_global"])
    }

    @Test
    fun `toAppError maps parser failure`() {
        val response =
            ApiResponse.error<Unit>(
                message = "Failed to parse",
                statusCode = 200,
                code = NetworkLocalCode.PARSER_ERROR,
            )

        val appError = response.toAppError()

        assertTrue(appError is AppError.Parsing)
        val parsing = appError as AppError.Parsing
        assertEquals(NetworkLocalCode.PARSER_ERROR, parsing.code)
    }

    @Test
    fun `toAppError maps server failure`() {
        val response =
            ApiResponse.error<Unit>(
                message = "Server unavailable",
                statusCode = 503,
                code = "SERVICE_UNAVAILABLE",
                traceId = "trace-503",
            )

        val appError = response.toAppError()

        assertTrue(appError is AppError.Server)
        val server = appError as AppError.Server
        assertEquals(503, server.statusCode)
        assertEquals("trace-503", server.traceId)
    }

    @Test
    fun `Throwable toAppError maps unknown host to network unavailable`() {
        val appError = UnknownHostException("offline").toAppError()

        assertTrue(appError is AppError.Network)
        val network = appError as AppError.Network
        assertEquals(NetworkLocalStatus.NO_INTERNET, network.statusCode)
        assertEquals(NetworkLocalCode.NO_INTERNET, network.code)
    }

    @Test
    fun `ApiException toAppError preserves validation details`() {
        val exception =
            ApiException(
                statusCode = 400,
                code = "VALIDATION_ERROR",
                traceId = "trace-400",
                message = "Invalid request",
                errors =
                    listOf(
                        ApiValidationError(
                            field = "password",
                            message = "Password is too short",
                        ),
                    ),
            )

        val appError = exception.toAppError()

        assertTrue(appError is AppError.Validation)
        val validation = appError as AppError.Validation
        assertEquals(listOf("Password is too short"), validation.fieldErrors["password"])
    }
}
