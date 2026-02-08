package dev.fikril.androidcorekit.core.network.error

import dev.fikril.androidcorekit.core.network.model.ApiResponse
import dev.fikril.androidcorekit.core.network.model.ApiValidationError

class ApiException(
    val statusCode: Int?,
    val code: String?,
    val traceId: String?,
    override val message: String,
    val errors: List<ApiValidationError>? = null,
) : RuntimeException(message) {
    companion object {
        fun from(response: ApiResponse<*>): ApiException =
            ApiException(
                statusCode = response.statusCode,
                code = response.code,
                traceId = response.traceId,
                message = response.message ?: "API request failed.",
                errors = response.errors,
            )
    }
}
