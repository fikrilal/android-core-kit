package dev.fikril.androidcorekit.core.session

import kotlinx.coroutines.flow.StateFlow

interface SessionManager {
    val state: StateFlow<SessionState>

    fun accessToken(): String?

    fun refreshToken(): String?

    suspend fun setTokens(tokens: SessionTokens)

    suspend fun setAuthenticated(userId: String? = null)

    suspend fun setUnauthenticated()

    suspend fun clearTokens()

    suspend fun logout() = setUnauthenticated()
}
