package dev.fikril.androidcorekit.session

import dev.fikril.androidcorekit.core.session.SessionManager
import dev.fikril.androidcorekit.core.session.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemorySessionManager @Inject constructor() : SessionManager {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)

    override val state: StateFlow<SessionState> = mutableState.asStateFlow()

    override suspend fun setAuthenticated(userId: String?) {
        mutableState.value = SessionState.Authenticated(userId = userId)
    }

    override suspend fun setUnauthenticated() {
        mutableState.value = SessionState.Unauthenticated
    }
}

