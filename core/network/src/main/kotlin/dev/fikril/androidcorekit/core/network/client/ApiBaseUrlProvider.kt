package dev.fikril.androidcorekit.core.network.client

import dev.fikril.androidcorekit.core.network.model.ApiHost

fun interface ApiBaseUrlProvider {
    fun baseUrl(host: ApiHost): String
}

class StaticApiBaseUrlProvider(
    private val baseUrl: String,
) : ApiBaseUrlProvider {
    override fun baseUrl(host: ApiHost): String = baseUrl
}

class MapApiBaseUrlProvider(
    private val baseUrls: Map<ApiHost, String>,
    private val fallbackBaseUrl: String? = null,
) : ApiBaseUrlProvider {
    override fun baseUrl(host: ApiHost): String =
        baseUrls[host]
            ?: fallbackBaseUrl
            ?: error("Missing base URL mapping for host=$host.")
}
