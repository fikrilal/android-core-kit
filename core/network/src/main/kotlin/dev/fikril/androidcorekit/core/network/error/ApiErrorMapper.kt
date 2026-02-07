package dev.fikril.androidcorekit.core.network.error

import dev.fikril.androidcorekit.core.common.AppError
import dev.fikril.androidcorekit.core.network.model.ApiResponse
import dev.fikril.androidcorekit.core.network.model.ApiValidationError
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun ApiResponse<*>.toAppError(): AppError =
    mapToAppError(
        statusCode = statusCode,
        code = code,
        message = message,
        traceId = traceId,
        errors = errors,
        cause = null,
    )

fun ApiException.toAppError(): AppError =
    mapToAppError(
        statusCode = statusCode,
        code = code,
        message = message,
        traceId = traceId,
        errors = errors,
        cause = this,
    )

fun Throwable.toAppError(): AppError =
    when (this) {
        is ApiException -> toAppError()
        is SocketTimeoutException ->
            AppError.Network(
                code = NetworkLocalCode.TIMEOUT,
                message = "Request timed out.",
                statusCode = NetworkLocalStatus.TIMEOUT,
                cause = this,
            )
        is UnknownHostException ->
            AppError.Network(
                code = NetworkLocalCode.NO_INTERNET,
                message = "No internet connection.",
                statusCode = NetworkLocalStatus.NO_INTERNET,
                cause = this,
            )
        is IOException ->
            AppError.Network(
                code = NetworkLocalCode.NETWORK_FAILURE,
                message = "Network request failed.",
                statusCode = NetworkLocalStatus.NETWORK_FAILURE,
                cause = this,
            )
        else -> AppError.Unknown(cause = this)
    }

private fun mapToAppError(
    statusCode: Int?,
    code: String?,
    message: String?,
    traceId: String?,
    errors: List<ApiValidationError>?,
    cause: Throwable?,
): AppError =
    when {
        statusCode == 401 || statusCode == 403 -> AppError.Unauthorized
        statusCode == 404 -> AppError.NotFound
        errors.orEmpty().isNotEmpty() || statusCode == 400 || statusCode == 422 ->
            AppError.Validation(
                message = message,
                fieldErrors = errors.toFieldErrorMap(),
                code = code,
            )
        code == NetworkLocalCode.PARSER_ERROR ->
            AppError.Parsing(
                message = message,
                code = code,
            )
        statusCode in 500..599 ->
            AppError.Server(
                statusCode = statusCode ?: 500,
                message = message,
                code = code,
                traceId = traceId,
            )
        statusCode in LOCAL_NETWORK_STATUS_CODES ->
            AppError.Network(
                code = code,
                message = message,
                statusCode = statusCode,
                traceId = traceId,
                cause = cause,
            )
        else -> AppError.Unknown(cause = cause)
    }

private fun List<ApiValidationError>?.toFieldErrorMap(): Map<String, List<String>> =
    this
        .orEmpty()
        .groupBy(
            keySelector = { error -> error.field ?: NON_FIELD_KEY },
            valueTransform = { error -> error.message },
        ).mapValues { (_, messages) -> messages.distinct() }

private val LOCAL_NETWORK_STATUS_CODES =
    setOf(
        NetworkLocalStatus.NO_INTERNET,
        NetworkLocalStatus.TIMEOUT,
        NetworkLocalStatus.NETWORK_FAILURE,
        NetworkLocalStatus.UNEXPECTED_FAILURE,
    )

private const val NON_FIELD_KEY = "_global"
