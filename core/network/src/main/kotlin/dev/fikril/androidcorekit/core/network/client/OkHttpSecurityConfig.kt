package dev.fikril.androidcorekit.core.network.client

import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient

fun OkHttpClient.Builder.applyNetworkSecurityConfig(config: NetworkSecurityConfig): OkHttpClient.Builder {
    if (config.enforceModernTls) {
        connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
    }

    if (config.pinningMode == NetworkPinningMode.ENFORCED) {
        val certificatePinnerBuilder = CertificatePinner.Builder()
        NetworkSecurityConfig
            .normalizePins(config.pinnedCertificatesByHost)
            .forEach { (host, pins) ->
                pins.forEach { pin ->
                    certificatePinnerBuilder.add(host, pin)
                }
            }
        certificatePinner(certificatePinnerBuilder.build())
    }

    return this
}
