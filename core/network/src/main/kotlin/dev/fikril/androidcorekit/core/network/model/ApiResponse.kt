package dev.fikril.androidcorekit.core.network.model

import kotlinx.serialization.json.JsonObject

enum class ApiStatus {
    SUCCESS,
    ERROR,
}

data class ApiValidationError(
    val field: String? = null,
    val message: String,
    val code: String? = null,
)

data class ApiResponse<T>(
    val status: ApiStatus,
    val data: T?,
    val message: String?,
    val code: String?,
    val traceId: String?,
    val meta: JsonObject?,
    val errors: List<ApiValidationError>?,
    val statusCode: Int?,
) {
    val isSuccess: Boolean
        get() = status == ApiStatus.SUCCESS

    val isError: Boolean
        get() = status == ApiStatus.ERROR

    fun fieldErrors(field: String): List<ApiValidationError> = errors.orEmpty().filter { it.field == field }

    companion object {
        fun <T> success(
            data: T,
            message: String? = null,
            code: String? = null,
            traceId: String? = null,
            meta: JsonObject? = null,
            statusCode: Int? = null,
        ): ApiResponse<T> =
            ApiResponse(
                status = ApiStatus.SUCCESS,
                data = data,
                message = message,
                code = code,
                traceId = traceId,
                meta = meta,
                errors = null,
                statusCode = statusCode,
            )

        fun <T> error(
            message: String,
            statusCode: Int? = null,
            code: String? = null,
            traceId: String? = null,
            errors: List<ApiValidationError>? = null,
            meta: JsonObject? = null,
        ): ApiResponse<T> =
            ApiResponse(
                status = ApiStatus.ERROR,
                data = null,
                message = message,
                code = code,
                traceId = traceId,
                meta = meta,
                errors = errors,
                statusCode = statusCode,
            )
    }
}
