package dev.fikril.androidcorekit.session

import dev.fikril.androidcorekit.core.common.AppDispatchers
import dev.fikril.androidcorekit.core.session.SessionManager
import dev.fikril.androidcorekit.core.session.SessionState
import dev.fikril.androidcorekit.core.session.SessionTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistedSessionManager
    @Inject
    constructor(
        private val sessionStore: SessionStore,
        private val appDispatchers: AppDispatchers,
    ) : SessionManager {
        private val stateMutex = Mutex()
        private val mutableState = MutableStateFlow<SessionState>(SessionState.Unknown)
        private val scope = CoroutineScope(SupervisorJob() + appDispatchers.io)

        @Volatile
        private var tokens: SessionTokens? = null

        override val state: StateFlow<SessionState> = mutableState.asStateFlow()

        init {
            scope.launch {
                hydrateFromStore()
            }
        }

        override fun accessToken(): String? = tokens?.accessToken

        override fun refreshToken(): String? = tokens?.refreshToken

        override suspend fun setTokens(tokens: SessionTokens) {
            val record =
                stateMutex.withLock {
                    this.tokens = tokens
                    val userId = (mutableState.value as? SessionState.Authenticated)?.userId
                    mutableState.value = SessionState.Authenticated(userId = userId)
                    PersistedSessionRecord(
                        accessToken = tokens.accessToken,
                        refreshToken = tokens.refreshToken,
                        userId = userId,
                    )
                }

            persistSafely(record)
        }

        override suspend fun setAuthenticated(userId: String?) {
            val record =
                stateMutex.withLock {
                    mutableState.value = SessionState.Authenticated(userId = userId)
                    tokens?.let {
                        PersistedSessionRecord(
                            accessToken = it.accessToken,
                            refreshToken = it.refreshToken,
                            userId = userId,
                        )
                    }
                }

            if (record != null) {
                persistSafely(record)
            } else {
                clearStoreSafely()
            }
        }

        override suspend fun setUnauthenticated() {
            stateMutex.withLock {
                tokens = null
                mutableState.value = SessionState.Unauthenticated
            }

            clearStoreSafely()
        }

        override suspend fun clearTokens() {
            stateMutex.withLock {
                tokens = null
            }

            clearStoreSafely()
        }

        private suspend fun hydrateFromStore() {
            val persistedState = runCatching { sessionStore.read() }

            persistedState
                .onSuccess { record ->
                    stateMutex.withLock {
                        if (mutableState.value !is SessionState.Unknown) {
                            return
                        }

                        if (record == null) {
                            tokens = null
                            mutableState.value = SessionState.Unauthenticated
                        } else {
                            tokens =
                                SessionTokens(
                                    accessToken = record.accessToken,
                                    refreshToken = record.refreshToken,
                                )
                            mutableState.value = SessionState.Authenticated(userId = record.userId)
                        }
                    }
                }.onFailure {
                    clearStoreSafely()
                    stateMutex.withLock {
                        if (mutableState.value is SessionState.Unknown) {
                            tokens = null
                            mutableState.value = SessionState.Unauthenticated
                        }
                    }
                }
        }

        private suspend fun persistSafely(record: PersistedSessionRecord) {
            runCatching {
                sessionStore.write(record)
            }.onFailure {
                stateMutex.withLock {
                    tokens = null
                    mutableState.value = SessionState.Unauthenticated
                }
                clearStoreSafely()
            }
        }

        private suspend fun clearStoreSafely() {
            runCatching {
                sessionStore.clear()
            }
        }
    }
