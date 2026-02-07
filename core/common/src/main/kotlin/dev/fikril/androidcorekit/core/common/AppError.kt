package dev.fikril.androidcorekit.core.common

sealed interface AppError {
    data class Network(
        val code: String? = null,
        val message: String? = null,
        val statusCode: Int? = null,
        val traceId: String? = null,
        val cause: Throwable? = null,
    ) : AppError

    data object Unauthorized : AppError

    data object NotFound : AppError

    data class Validation(
        val message: String? = null,
        val fieldErrors: Map<String, List<String>> = emptyMap(),
        val code: String? = null,
    ) : AppError

    data class Parsing(
        val message: String? = null,
        val code: String? = null,
    ) : AppError

    data class Server(
        val statusCode: Int,
        val message: String? = null,
        val code: String? = null,
        val traceId: String? = null,
    ) : AppError

    data class Unknown(
        val cause: Throwable? = null,
    ) : AppError
}
