package dev.fikril.androidcorekit.network

import dev.fikril.androidcorekit.core.network.client.NetworkPinningMode
import dev.fikril.androidcorekit.core.network.client.NetworkSecurityConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkSecurityPolicyFactory
    @Inject
    constructor() {
        fun create(
            baseUrl: String,
            isProd: Boolean,
            primaryPin: String,
            backupPin: String,
        ): NetworkSecurityConfig {
            if (!isProd) {
                return NetworkSecurityConfig(
                    pinningMode = NetworkPinningMode.RELAXED,
                    enforceModernTls = false,
                )
            }

            val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
            val host = checkNotNull(normalizedBaseUrl.toHttpUrlOrNull()) { "Invalid BASE_URL." }.host
            val pins = setOf(primaryPin, backupPin).filter { it.isNotBlank() }.toSet()

            require(pins.size >= 2) {
                "Prod transport security requires primary and backup certificate pins."
            }

            return NetworkSecurityConfig(
                pinningMode = NetworkPinningMode.ENFORCED,
                pinnedCertificatesByHost = mapOf(host to pins),
                enforceModernTls = true,
            )
        }

        private fun normalizeBaseUrl(baseUrl: String): String =
            if (baseUrl.endsWith('/')) {
                baseUrl
            } else {
                "$baseUrl/"
            }
    }
