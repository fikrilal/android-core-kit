package dev.fikril.androidcorekit.core.network.client

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.fikril.androidcorekit.core.network.model.ApiHost
import dev.fikril.androidcorekit.core.network.serialization.defaultNetworkJson
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentHashMap

class RetrofitServiceFactory(
    private val baseUrlProvider: ApiBaseUrlProvider,
    private val okHttpClient: OkHttpClient,
    private val json: Json = defaultNetworkJson(),
) {
    constructor(
        baseUrlProvider: ApiBaseUrlProvider,
        networkClientFactory: NetworkClientFactory,
        json: Json = defaultNetworkJson(),
    ) : this(
        baseUrlProvider = baseUrlProvider,
        okHttpClient = networkClientFactory.create(),
        json = json,
    )

    private val retrofitCache = ConcurrentHashMap<ApiHost, Retrofit>()

    fun retrofit(host: ApiHost): Retrofit =
        retrofitCache.getOrPut(host) {
            Retrofit
                .Builder()
                .baseUrl(requireValidBaseUrl(host))
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
                .build()
        }

    inline fun <reified T : Any> create(host: ApiHost): T = retrofit(host).create(T::class.java)

    private fun requireValidBaseUrl(host: ApiHost): String {
        val baseUrl = normalizeBaseUrl(baseUrlProvider.baseUrl(host))
        checkNotNull(baseUrl.toHttpUrlOrNull()) {
            "Invalid base URL for host=$host."
        }
        return baseUrl
    }

    private fun normalizeBaseUrl(baseUrl: String): String = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
