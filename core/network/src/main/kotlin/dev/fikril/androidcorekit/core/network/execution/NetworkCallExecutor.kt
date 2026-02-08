package dev.fikril.androidcorekit.core.network.execution

import dev.fikril.androidcorekit.core.common.AppDispatchers
import dev.fikril.androidcorekit.core.network.error.ApiException
import dev.fikril.androidcorekit.core.network.error.NetworkLocalCode
import dev.fikril.androidcorekit.core.network.error.NetworkLocalStatus
import dev.fikril.androidcorekit.core.network.model.ApiResponse
import dev.fikril.androidcorekit.core.network.model.ApiValidationError
import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import dev.fikril.androidcorekit.core.network.serialization.JsonParser
import dev.fikril.androidcorekit.core.network.serialization.defaultNetworkJson
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkCallExecutor(
    private val dispatchers: AppDispatchers,
    private val json: Json = defaultNetworkJson(),
) {
    suspend fun <T> execute(
        call: suspend () -> Response<ResponseBody>,
        parser: JsonParser<T>,
        throwOnError: Boolean = true,
    ): ApiResponse<T> {
        val response = executeSafely(call = call, parser = parser)
        if (response.isError && throwOnError) {
            throw ApiException.from(response)
        }
        return response
    }

    private suspend fun <T> executeSafely(
        call: suspend () -> Response<ResponseBody>,
        parser: JsonParser<T>,
    ): ApiResponse<T> =
        withContext(dispatchers.io) {
            runCatching { call() }.fold(
                onSuccess = { response -> parseResponse(response = response, parser = parser) },
                onFailure = { throwable -> mapTransportFailure(throwable) },
            )
        }

    private fun <T> parseResponse(
        response: Response<ResponseBody>,
        parser: JsonParser<T>,
    ): ApiResponse<T> {
        val statusCode = response.code()
        val traceId = response.headers()[NetworkHeaders.REQUEST_ID]

        return if (response.isSuccessful) {
            parseSuccess(
                parser = parser,
                statusCode = statusCode,
                traceId = traceId,
                rawBody = response.body()?.string(),
            )
        } else {
            parseError(
                statusCode = statusCode,
                traceId = traceId,
                rawBody = response.errorBody()?.string(),
            )
        }
    }

    private fun <T> parseSuccess(
        parser: JsonParser<T>,
        statusCode: Int,
        traceId: String?,
        rawBody: String?,
    ): ApiResponse<T> {
        val parsedJson = parseJsonSafely(rawBody)
        val envelope = parseSuccessEnvelope(parsedJson)

        return runCatching {
            ApiResponse.success(
                data = parser(envelope.data),
                message = envelope.message,
                code = envelope.code,
                traceId = envelope.traceId ?: traceId,
                meta = envelope.meta,
                statusCode = statusCode,
            )
        }.getOrElse { throwable ->
            ApiResponse.error(
                message = "Failed to parse API success payload: ${throwable.message}",
                statusCode = statusCode,
                code = NetworkLocalCode.PARSER_ERROR,
                traceId = traceId,
            )
        }
    }

    private fun <T> parseError(
        statusCode: Int,
        traceId: String?,
        rawBody: String?,
    ): ApiResponse<T> {
        val parsedJson = parseJsonSafely(rawBody)
        val jsonObject = parsedJson as? JsonObject

        val message =
            jsonObject.string("title")
                ?: jsonObject.string("detail")
                ?: jsonObject.string("message")
                ?: jsonObject.string("error")
                ?: rawBody?.takeIf { it.isNotBlank() }
                ?: "Request failed with status $statusCode."

        val code = jsonObject.string("code")
        val responseTraceId = jsonObject.string("traceId") ?: traceId
        val meta = jsonObject?.get("meta") as? JsonObject
        val errors = jsonObject.parseValidationErrors()

        return ApiResponse.error(
            message = message,
            statusCode = statusCode,
            code = code,
            traceId = responseTraceId,
            errors = errors,
            meta = meta,
        )
    }

    private fun parseSuccessEnvelope(jsonElement: JsonElement): ParsedEnvelope {
        val jsonObject = jsonElement as? JsonObject
        val hasEnvelope = jsonObject?.containsKey("data") == true
        return if (hasEnvelope) {
            ParsedEnvelope(
                data = jsonObject["data"] ?: JsonNull,
                meta = jsonObject["meta"] as? JsonObject,
                message = jsonObject.string("message"),
                code = jsonObject.string("code"),
                traceId = jsonObject.string("traceId"),
            )
        } else {
            ParsedEnvelope(data = jsonElement)
        }
    }

    private fun parseJsonSafely(rawBody: String?): JsonElement {
        if (rawBody.isNullOrBlank()) return JsonNull
        return runCatching { json.parseToJsonElement(rawBody) }.getOrElse { JsonPrimitive(rawBody) }
    }

    private fun <T> mapTransportFailure(throwable: Throwable): ApiResponse<T> {
        val localError =
            when (throwable) {
                is SocketTimeoutException ->
                    LocalError(
                        statusCode = NetworkLocalStatus.TIMEOUT,
                        code = NetworkLocalCode.TIMEOUT,
                        message = "Request timed out.",
                    )
                is UnknownHostException ->
                    LocalError(
                        statusCode = NetworkLocalStatus.NO_INTERNET,
                        code = NetworkLocalCode.NO_INTERNET,
                        message = "No internet connection.",
                    )
                is IOException ->
                    LocalError(
                        statusCode = NetworkLocalStatus.NETWORK_FAILURE,
                        code = NetworkLocalCode.NETWORK_FAILURE,
                        message = "Network request failed.",
                    )
                else ->
                    LocalError(
                        statusCode = NetworkLocalStatus.UNEXPECTED_FAILURE,
                        code = NetworkLocalCode.UNEXPECTED_FAILURE,
                        message = "Unexpected network failure.",
                    )
            }

        return ApiResponse.error(
            message = localError.message,
            statusCode = localError.statusCode,
            code = localError.code,
        )
    }

    private fun JsonObject?.string(key: String): String? =
        this
            ?.get(key)
            ?.let { element ->
                (element as? JsonPrimitive)?.contentOrNull
            }?.takeIf { it.isNotBlank() }

    private fun JsonObject?.parseValidationErrors(): List<ApiValidationError>? {
        val errorArray = this?.get("errors") as? JsonArray ?: return null
        val errors =
            errorArray.mapNotNull { element ->
                val item = element as? JsonObject ?: return@mapNotNull null
                val message = item.string("message") ?: item.string("detail") ?: return@mapNotNull null
                ApiValidationError(
                    field = item.string("field") ?: item.string("path"),
                    message = message,
                    code = item.string("code"),
                )
            }
        return errors.ifEmpty { null }
    }

    private data class ParsedEnvelope(
        val data: JsonElement,
        val meta: JsonObject? = null,
        val message: String? = null,
        val code: String? = null,
        val traceId: String? = null,
    )

    private data class LocalError(
        val statusCode: Int,
        val code: String,
        val message: String,
    )
}
