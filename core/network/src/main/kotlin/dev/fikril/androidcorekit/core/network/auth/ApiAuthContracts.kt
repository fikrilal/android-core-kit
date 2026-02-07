package dev.fikril.androidcorekit.core.network

fun interface AccessTokenProvider {
    fun accessToken(): String?
}

fun interface AccessTokenRefresher {
    suspend fun refreshAccessToken(): Boolean
}
