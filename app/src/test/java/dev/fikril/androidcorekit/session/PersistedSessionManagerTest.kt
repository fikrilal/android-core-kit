package dev.fikril.androidcorekit.session

import dev.fikril.androidcorekit.core.common.AppDispatchers
import dev.fikril.androidcorekit.core.session.SessionState
import dev.fikril.androidcorekit.core.session.SessionTokens
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersistedSessionManagerTest {
    @Test
    fun `hydrates authenticated state from persisted storage`() =
        runTest {
            val scheduler = TestCoroutineScheduler()
            val dispatcher = StandardTestDispatcher(scheduler)
            val store = FakeSessionStore()
            store.persistedRecord =
                PersistedSessionRecord(
                    accessToken = "access-token",
                    refreshToken = "refresh-token",
                    userId = "user-1",
                )

            val manager =
                PersistedSessionManager(
                    sessionStore = store,
                    appDispatchers = SessionManagerTestDispatchers(dispatcher),
                )

            scheduler.advanceUntilIdle()

            assertEquals(SessionState.Authenticated(userId = "user-1"), manager.state.value)
            assertEquals("access-token", manager.accessToken())
            assertEquals("refresh-token", manager.refreshToken())
        }

    @Test
    fun `falls back to unauthenticated and clears store when hydration fails`() =
        runTest {
            val scheduler = TestCoroutineScheduler()
            val dispatcher = StandardTestDispatcher(scheduler)
            val store = FakeSessionStore(readFailure = IllegalStateException("broken storage"))

            val manager =
                PersistedSessionManager(
                    sessionStore = store,
                    appDispatchers = SessionManagerTestDispatchers(dispatcher),
                )

            scheduler.advanceUntilIdle()

            assertEquals(SessionState.Unauthenticated, manager.state.value)
            assertTrue(store.clearCalls > 0)
            assertNull(manager.accessToken())
            assertNull(manager.refreshToken())
        }

    @Test
    fun `setUnauthenticated clears memory and persisted tokens`() =
        runTest {
            val scheduler = TestCoroutineScheduler()
            val dispatcher = StandardTestDispatcher(scheduler)
            val store = FakeSessionStore()
            val manager =
                PersistedSessionManager(
                    sessionStore = store,
                    appDispatchers = SessionManagerTestDispatchers(dispatcher),
                )

            scheduler.advanceUntilIdle()
            manager.setTokens(
                SessionTokens(
                    accessToken = "access-token",
                    refreshToken = "refresh-token",
                ),
            )
            manager.setAuthenticated(userId = "user-1")
            manager.setUnauthenticated()

            assertEquals(SessionState.Unauthenticated, manager.state.value)
            assertNull(manager.accessToken())
            assertNull(manager.refreshToken())
            assertEquals(null, store.persistedRecord)
            assertTrue(store.clearCalls > 0)
        }
}

private data class SessionManagerTestDispatchers(
    private val dispatcher: CoroutineDispatcher,
) : AppDispatchers {
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
}

private class FakeSessionStore(
    private val readFailure: Throwable? = null,
) : SessionStore {
    var persistedRecord: PersistedSessionRecord? = null
    var clearCalls: Int = 0

    override suspend fun read(): PersistedSessionRecord? {
        readFailure?.let { throw it }
        return persistedRecord
    }

    override suspend fun write(record: PersistedSessionRecord) {
        persistedRecord = record
    }

    override suspend fun clear() {
        clearCalls += 1
        persistedRecord = null
    }
}
