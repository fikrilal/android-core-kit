package dev.fikril.androidcorekit.core.session

import kotlinx.coroutines.flow.StateFlow

interface SessionManager {
    val state: StateFlow<SessionState>

    suspend fun setAuthenticated(userId: String? = null)

    suspend fun setUnauthenticated()

    suspend fun logout() = setUnauthenticated()
}
