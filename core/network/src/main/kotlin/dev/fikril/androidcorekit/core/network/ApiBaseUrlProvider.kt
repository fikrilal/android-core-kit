package dev.fikril.androidcorekit.core.network

fun interface ApiBaseUrlProvider {
    fun baseUrl(host: ApiHost): String
}

class StaticApiBaseUrlProvider(
    private val baseUrl: String,
) : ApiBaseUrlProvider {
    override fun baseUrl(host: ApiHost): String = baseUrl
}
