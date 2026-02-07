package dev.fikril.androidcorekit.core.network.telemetry

enum class NetworkOutcome {
    SUCCESS,
    HTTP_ERROR,
    NETWORK_ERROR,
}

data class NetworkTelemetryEvent(
    val method: String,
    val url: String,
    val requestId: String?,
    val outcome: NetworkOutcome,
    val statusCode: Int?,
    val attemptCount: Int,
    val durationMillis: Long,
    val errorMessage: String? = null,
)

fun interface NetworkTelemetryObserver {
    fun onRequestComplete(event: NetworkTelemetryEvent)
}

object NoOpNetworkTelemetryObserver : NetworkTelemetryObserver {
    override fun onRequestComplete(event: NetworkTelemetryEvent) = Unit
}
