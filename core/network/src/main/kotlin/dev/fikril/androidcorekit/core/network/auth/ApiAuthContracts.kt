package dev.fikril.androidcorekit.core.network.auth

fun interface AccessTokenProvider {
    fun accessToken(): String?
}

fun interface AccessTokenRefresher {
    suspend fun refreshAccessToken(): Boolean
}

object NoOpAccessTokenProvider : AccessTokenProvider {
    override fun accessToken(): String? = null
}

object NoOpAccessTokenRefresher : AccessTokenRefresher {
    override suspend fun refreshAccessToken(): Boolean = false
}
