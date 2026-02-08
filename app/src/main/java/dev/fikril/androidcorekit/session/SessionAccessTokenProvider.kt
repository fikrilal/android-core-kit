package dev.fikril.androidcorekit.session

import dev.fikril.androidcorekit.core.network.auth.AccessTokenProvider
import dev.fikril.androidcorekit.core.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionAccessTokenProvider
    @Inject
    constructor(
        private val sessionManager: SessionManager,
    ) : AccessTokenProvider {
        override fun accessToken(): String? = sessionManager.accessToken()
    }
