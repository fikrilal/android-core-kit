package dev.fikril.androidcorekit.core.network

import dev.fikril.androidcorekit.core.common.AppDispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class ApiHelper(
    private val client: OkHttpClient,
    private val baseUrlProvider: ApiBaseUrlProvider,
    private val dispatchers: AppDispatchers,
    private val accessTokenProvider: AccessTokenProvider = AccessTokenProvider { null },
    private val accessTokenRefresher: AccessTokenRefresher? = null,
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
) {
    suspend fun <T> get(
        path: String,
        parser: JsonParser<T>,
        host: ApiHost = ApiHost.CORE,
        queryParameters: Map<String, Any?>? = null,
        headers: Map<String, String> = emptyMap(),
        throwOnError: Boolean = true,
        requiresAuth: Boolean = true,
    ): ApiResponse<T> =
        request(
            method = "GET",
            path = path,
            parser = parser,
            host = host,
            queryParameters = queryParameters,
            headers = headers,
            throwOnError = throwOnError,
            requiresAuth = requiresAuth,
        )

    suspend fun <T> post(
        path: String,
        parser: JsonParser<T>,
        host: ApiHost = ApiHost.CORE,
        data: Any? = null,
        queryParameters: Map<String, Any?>? = null,
        headers: Map<String, String> = emptyMap(),
        throwOnError: Boolean = true,
        requiresAuth: Boolean = true,
    ): ApiResponse<T> =
        request(
            method = "POST",
            path = path,
            parser = parser,
            host = host,
            data = data,
            queryParameters = queryParameters,
            headers = headers,
            throwOnError = throwOnError,
            requiresAuth = requiresAuth,
        )

    suspend fun <T> put(
        path: String,
        parser: JsonParser<T>,
        host: ApiHost = ApiHost.CORE,
        data: Any? = null,
        queryParameters: Map<String, Any?>? = null,
        headers: Map<String, String> = emptyMap(),
        throwOnError: Boolean = true,
        requiresAuth: Boolean = true,
    ): ApiResponse<T> =
        request(
            method = "PUT",
            path = path,
            parser = parser,
            host = host,
            data = data,
            queryParameters = queryParameters,
            headers = headers,
            throwOnError = throwOnError,
            requiresAuth = requiresAuth,
        )

    suspend fun <T> patch(
        path: String,
        parser: JsonParser<T>,
        host: ApiHost = ApiHost.CORE,
        data: Any? = null,
        queryParameters: Map<String, Any?>? = null,
        headers: Map<String, String> = emptyMap(),
        throwOnError: Boolean = true,
        requiresAuth: Boolean = true,
    ): ApiResponse<T> =
        request(
            method = "PATCH",
            path = path,
            parser = parser,
            host = host,
            data = data,
            queryParameters = queryParameters,
            headers = headers,
            throwOnError = throwOnError,
            requiresAuth = requiresAuth,
        )

    suspend fun <T> delete(
        path: String,
        parser: JsonParser<T>,
        host: ApiHost = ApiHost.CORE,
        data: Any? = null,
        queryParameters: Map<String, Any?>? = null,
        headers: Map<String, String> = emptyMap(),
        throwOnError: Boolean = true,
        requiresAuth: Boolean = true,
    ): ApiResponse<T> =
        request(
            method = "DELETE",
            path = path,
            parser = parser,
            host = host,
            data = data,
            queryParameters = queryParameters,
            headers = headers,
            throwOnError = throwOnError,
            requiresAuth = requiresAuth,
        )

    private suspend fun <T> request(
        method: String,
        path: String,
        parser: JsonParser<T>,
        host: ApiHost,
        data: Any? = null,
        queryParameters: Map<String, Any?>? = null,
        headers: Map<String, String>,
        throwOnError: Boolean,
        requiresAuth: Boolean,
    ): ApiResponse<T> {
        val url = buildUrl(host = host, path = path, queryParameters = queryParameters)
        val requestBody = data.toRequestBody()
        val initialRequest =
            buildRequest(
                method = method,
                url = url,
                requestBody = requestBody,
                headers = headers,
                requiresAuth = requiresAuth,
            )

        var response = executeRequest(request = initialRequest, parser = parser)
        val canRetry =
            shouldAttemptRefreshRetry(
                method = method,
                headers = headers,
                requiresAuth = requiresAuth,
                statusCode = response.statusCode,
            )

        if (canRetry && accessTokenRefresher?.refreshAccessToken() == true) {
            val retryRequest =
                buildRequest(
                    method = method,
                    url = url,
                    requestBody = requestBody,
                    headers = headers,
                    requiresAuth = requiresAuth,
                )
            response = executeRequest(request = retryRequest, parser = parser)
        }

        if (response.isError && throwOnError) {
            throw ApiException.from(response)
        }

        return response
    }

    private fun buildUrl(
        host: ApiHost,
        path: String,
        queryParameters: Map<String, Any?>?,
    ): HttpUrl {
        val baseUrl =
            baseUrlProvider.baseUrl(host).toHttpUrlOrNull()
                ?: error("Invalid base URL for $host.")
        val normalizedPath = path.trimStart('/')

        return baseUrl
            .newBuilder()
            .addPathSegments(normalizedPath)
            .apply {
                queryParameters.orEmpty().forEach { (key, value) ->
                    if (value != null) {
                        addQueryParameter(key, value.toString())
                    }
                }
            }.build()
    }

    private fun buildRequest(
        method: String,
        url: HttpUrl,
        requestBody: RequestBody?,
        headers: Map<String, String>,
        requiresAuth: Boolean,
    ): Request =
        Request
            .Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .apply {
                headers.forEach { (name, value) ->
                    addHeader(name, value)
                }
                if (requiresAuth) {
                    accessTokenProvider.accessToken()?.let { token ->
                        header("Authorization", "Bearer $token")
                    }
                }
            }.method(method, requestBodyForMethod(method, requestBody))
            .build()

    private fun requestBodyForMethod(
        method: String,
        requestBody: RequestBody?,
    ): RequestBody? {
        val uppercaseMethod = method.uppercase()
        val allowsBody = uppercaseMethod in BODY_METHODS
        if (!allowsBody) return null
        return requestBody ?: EMPTY_JSON_BODY
    }

    private suspend fun <T> executeRequest(
        request: Request,
        parser: JsonParser<T>,
    ): ApiResponse<T> =
        withContext(dispatchers.io) {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string()
                val statusCode = response.code
                val traceId = response.requestId()
                if (response.isSuccessful) {
                    parseSuccess(
                        parser = parser,
                        statusCode = statusCode,
                        traceId = traceId,
                        rawBody = rawBody,
                    )
                } else {
                    parseError(
                        statusCode = statusCode,
                        traceId = traceId,
                        rawBody = rawBody,
                    )
                }
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
                code = "PARSER_ERROR",
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

    private fun shouldAttemptRefreshRetry(
        method: String,
        headers: Map<String, String>,
        requiresAuth: Boolean,
        statusCode: Int?,
    ): Boolean {
        val refreshRetryEligible =
            requiresAuth &&
                statusCode == HTTP_UNAUTHORIZED &&
                accessTokenRefresher != null
        if (!refreshRetryEligible) return false

        val uppercaseMethod = method.uppercase()
        val hasIdempotencyKeyHeader =
            headers.keys.any { key -> key.equals(IDEMPOTENCY_KEY_HEADER, ignoreCase = true) }
        val methodAllowsRetry =
            uppercaseMethod in SAFE_RETRY_METHODS ||
                (uppercaseMethod in WRITE_RETRY_METHODS && hasIdempotencyKeyHeader)
        return methodAllowsRetry
    }

    private fun Any?.toRequestBody(): RequestBody? {
        if (this == null) return null

        val jsonText =
            when (this) {
                is String -> this
                is JsonElement -> json.encodeToString(JsonElement.serializer(), this)
                else -> json.encodeToString(JsonElement.serializer(), toJsonElement())
            }

        return jsonText.toRequestBody(JSON_MEDIA_TYPE)
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.toJsonElement(): JsonElement =
        when (this) {
            null -> JsonNull
            is JsonElement -> this
            is String -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            is Map<*, *> ->
                JsonObject(
                    entries
                        .filter { (key, _) -> key is String }
                        .associate { (key, value) -> key as String to value.toJsonElement() },
                )
            is Iterable<*> -> JsonArray(map { item -> item.toJsonElement() })
            else -> JsonPrimitive(toString())
        }

    private fun Response.requestId(): String? = header("x-request-id") ?: header("X-Request-Id")

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

    companion object {
        fun create(
            baseUrlProvider: ApiBaseUrlProvider,
            dispatchers: AppDispatchers,
            accessTokenProvider: AccessTokenProvider = AccessTokenProvider { null },
            accessTokenRefresher: AccessTokenRefresher? = null,
        ): ApiHelper =
            ApiHelper(
                client = OkHttpClient.Builder().build(),
                baseUrlProvider = baseUrlProvider,
                dispatchers = dispatchers,
                accessTokenProvider = accessTokenProvider,
                accessTokenRefresher = accessTokenRefresher,
            )

        private const val HTTP_UNAUTHORIZED = 401
        private const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val SAFE_RETRY_METHODS = setOf("GET", "HEAD", "OPTIONS")
        private val WRITE_RETRY_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        private val BODY_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        private val EMPTY_JSON_BODY = "{}".toRequestBody(JSON_MEDIA_TYPE)
    }
}
