package dev.fikril.androidcorekit.core.network.client

enum class NetworkPinningMode {
    RELAXED,
    ENFORCED,
}

data class NetworkSecurityConfig(
    val pinningMode: NetworkPinningMode = NetworkPinningMode.RELAXED,
    val pinnedCertificatesByHost: Map<String, Set<String>> = emptyMap(),
    val enforceModernTls: Boolean = false,
) {
    init {
        val normalizedPins = normalizePins(pinnedCertificatesByHost)
        if (pinningMode == NetworkPinningMode.ENFORCED) {
            require(normalizedPins.isNotEmpty()) {
                "ENFORCED pinning mode requires at least one pinned host."
            }
        }
    }

    companion object {
        fun normalizePins(hostPins: Map<String, Set<String>>): Map<String, Set<String>> =
            hostPins
                .mapValues { (_, pins) -> pins.filter { it.isNotBlank() }.toSet() }
                .filterValues { it.isNotEmpty() }
                .also { normalized ->
                    normalized.forEach { (host, pins) ->
                        require("//" !in host) {
                            "Pinned host must not include a scheme: $host"
                        }
                        pins.forEach { pin ->
                            require(pin.startsWith("sha256/")) {
                                "Pinned certificate hash must use sha256/ prefix."
                            }
                        }
                    }
                }
    }
}
