package dev.fikril.androidcorekit.session

data class PersistedSessionRecord(
    val accessToken: String,
    val refreshToken: String,
    val userId: String?,
)

interface SessionStore {
    suspend fun read(): PersistedSessionRecord?

    suspend fun write(record: PersistedSessionRecord)

    suspend fun clear()
}
