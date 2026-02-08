package dev.fikril.androidcorekit.core.session

sealed interface SessionState {
    data object Unknown : SessionState

    data object Unauthenticated : SessionState

    data class Authenticated(
        val userId: String? = null,
    ) : SessionState
}
