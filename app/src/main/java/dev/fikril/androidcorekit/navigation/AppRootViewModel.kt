package dev.fikril.androidcorekit.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fikril.androidcorekit.core.session.SessionManager
import dev.fikril.androidcorekit.core.session.SessionState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppRootViewModel
    @Inject
    constructor(
        sessionManager: SessionManager,
    ) : ViewModel() {
        val sessionState: StateFlow<SessionState> = sessionManager.state
    }
