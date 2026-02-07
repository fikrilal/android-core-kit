package dev.fikril.androidcorekit.core.network.telemetry

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

enum class NetworkOutcome {
    SUCCESS,
    HTTP_ERROR,
    NETWORK_ERROR,
}

enum class NetworkTelemetryEventType {
    REQUEST_COMPLETE,
    RETRY_ATTEMPT,
    REFRESH_ATTEMPT,
    REFRESH_RESULT,
}

enum class NetworkRefreshOutcome {
    SUCCESS,
    FAILURE,
    SKIPPED,
}

data class NetworkTelemetryEvent(
    val eventType: NetworkTelemetryEventType,
    val method: String,
    val route: String,
    val requestId: String?,
    val traceId: String? = null,
    val outcome: NetworkOutcome? = null,
    val refreshOutcome: NetworkRefreshOutcome? = null,
    val statusCode: Int?,
    val attemptCount: Int,
    val durationMillis: Long? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
)

fun interface NetworkTelemetryObserver {
    fun onEvent(event: NetworkTelemetryEvent)
}

object NoOpNetworkTelemetryObserver : NetworkTelemetryObserver {
    override fun onEvent(event: NetworkTelemetryEvent) = Unit
}

object NetworkTelemetrySanitizer {
    fun sanitizeRoute(url: String): String {
        val httpUrl = url.toHttpUrlOrNull()
        if (httpUrl != null) {
            val portSuffix =
                when (httpUrl.port) {
                    80,
                    443,
                    -1,
                    -> ""
                    else -> ":${httpUrl.port}"
                }
            return "${httpUrl.host}$portSuffix${httpUrl.encodedPath}"
        }

        val withoutFragment = url.substringBefore('#')
        return withoutFragment.substringBefore('?')
    }

    fun sanitizeText(message: String?): String? {
        if (message.isNullOrBlank()) return message
        return message
            .replace(BEARER_TOKEN_REGEX, "Bearer [REDACTED]")
            .replace(EMAIL_REGEX, "[REDACTED_EMAIL]")
            .replace(JWT_REGEX, "[REDACTED_JWT]")
            .replace(JSON_TOKEN_REGEX, "$1[REDACTED]$3")
            .replace(KEY_VALUE_TOKEN_REGEX, "$1=[REDACTED]")
    }

    private val BEARER_TOKEN_REGEX = Regex("(?i)Bearer\\s+[A-Za-z0-9\\-._~+/]+=*")
    private val EMAIL_REGEX = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val JWT_REGEX = Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
    private val JSON_TOKEN_REGEX = Regex("(?i)(\"(?:access|refresh)[_-]?token\"\\s*:\\s*\")([^\"]+)(\")")
    private val KEY_VALUE_TOKEN_REGEX = Regex("(?i)((?:access|refresh)[_-]?token)\\s*=\\s*([^\\s,;]+)")
}
