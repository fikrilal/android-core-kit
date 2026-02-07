package dev.fikril.androidcorekit.core.network.interceptor

import dev.fikril.androidcorekit.core.network.model.NetworkHeaders
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class RequestIdInterceptor(
    private val requestIdGenerator: () -> String = { UUID.randomUUID().toString() },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestId = request.header(NetworkHeaders.REQUEST_ID)
        if (!requestId.isNullOrBlank()) {
            return chain.proceed(request)
        }

        val updatedRequest =
            request
                .newBuilder()
                .header(NetworkHeaders.REQUEST_ID, requestIdGenerator())
                .build()

        return chain.proceed(updatedRequest)
    }
}
