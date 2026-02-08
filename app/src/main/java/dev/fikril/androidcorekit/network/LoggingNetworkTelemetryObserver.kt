package dev.fikril.androidcorekit.network

import android.util.Log
import dev.fikril.androidcorekit.core.network.telemetry.NetworkOutcome
import dev.fikril.androidcorekit.core.network.telemetry.NetworkRefreshOutcome
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryEvent
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryObserver
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetrySanitizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoggingNetworkTelemetryObserver
    @Inject
    constructor() : NetworkTelemetryObserver {
        override fun onEvent(event: NetworkTelemetryEvent) {
            val payload = event.toLogPayload()
            if (event.isWarning()) {
                Log.w(TAG, payload)
            } else {
                Log.i(TAG, payload)
            }
        }

        private fun NetworkTelemetryEvent.toLogPayload(): String =
            buildString {
                append("event=").append(eventType.name)
                append(" method=").append(method)
                append(" route=").append(route)
                append(" requestId=").append(requestId ?: "none")
                append(" traceId=").append(traceId ?: "none")
                append(" attempt=").append(attemptCount)
                append(" status=").append(statusCode ?: "none")
                append(" durationMs=").append(durationMillis ?: "none")
                append(" outcome=").append(outcome?.name ?: "none")
                append(" refreshOutcome=").append(refreshOutcome?.name ?: "none")
                append(" errorCode=").append(errorCode ?: "none")
                append(" error=")
                append(NetworkTelemetrySanitizer.sanitizeText(errorMessage) ?: "none")
            }

        private fun NetworkTelemetryEvent.isWarning(): Boolean =
            statusCode.let { status ->
                when {
                    refreshOutcome == NetworkRefreshOutcome.FAILURE -> true
                    outcome == NetworkOutcome.NETWORK_ERROR -> true
                    status != null && status >= 500 -> true
                    else -> false
                }
            }

        private companion object {
            private const val TAG = "NetworkTelemetry"
        }
    }
