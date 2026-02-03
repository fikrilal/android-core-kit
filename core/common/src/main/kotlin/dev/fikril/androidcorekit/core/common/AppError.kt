package dev.fikril.androidcorekit.core.common

sealed interface AppError {
    data class Network(
        val cause: Throwable? = null,
    ) : AppError

    data object Unauthorized : AppError

    data object NotFound : AppError

    data class Validation(
        val message: String? = null,
    ) : AppError

    data class Unknown(
        val cause: Throwable? = null,
    ) : AppError
}
