package dev.fikril.androidcorekit.session

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreEncryptedSessionStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val sessionCrypto: SessionCrypto,
    ) : SessionStore {
        private val dataStore: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                produceFile = { context.preferencesDataStoreFile(SESSION_FILE_NAME) },
            )

        override suspend fun read(): PersistedSessionRecord? {
            val preferences =
                dataStore.data
                    .catch { emit(emptyPreferences()) }
                    .first()

            val encryptedData = preferences[ENCRYPTED_DATA_KEY]
            val iv = preferences[INITIALIZATION_VECTOR_KEY]
            if (encryptedData == null || iv == null) {
                return null
            }

            val decryptedBytes =
                sessionCrypto.decrypt(
                    EncryptedPayload(
                        ciphertext = decode(encryptedData),
                        iv = decode(iv),
                    ),
                )

            return deserialize(decryptedBytes)
        }

        override suspend fun write(record: PersistedSessionRecord) {
            val serialized = serialize(record)
            val encryptedPayload = sessionCrypto.encrypt(serialized)

            dataStore.edit { preferences ->
                preferences[ENCRYPTED_DATA_KEY] = encode(encryptedPayload.ciphertext)
                preferences[INITIALIZATION_VECTOR_KEY] = encode(encryptedPayload.iv)
            }
        }

        override suspend fun clear() {
            dataStore.edit { preferences ->
                preferences.remove(ENCRYPTED_DATA_KEY)
                preferences.remove(INITIALIZATION_VECTOR_KEY)
            }
        }

        private fun serialize(record: PersistedSessionRecord): ByteArray {
            val outputStream = ByteArrayOutputStream()
            DataOutputStream(outputStream).use { output ->
                output.writeInt(FORMAT_VERSION)
                output.writeSizedString(record.accessToken)
                output.writeSizedString(record.refreshToken)
                output.writeNullableSizedString(record.userId)
            }
            return outputStream.toByteArray()
        }

        private fun deserialize(bytes: ByteArray): PersistedSessionRecord {
            val inputStream = ByteArrayInputStream(bytes)
            DataInputStream(inputStream).use { input ->
                val formatVersion = input.readInt()
                check(formatVersion == FORMAT_VERSION) {
                    "Unsupported session persistence format version: $formatVersion"
                }

                val accessToken = input.readSizedString()
                val refreshToken = input.readSizedString()
                val userId = input.readNullableSizedString()

                return PersistedSessionRecord(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId,
                )
            }
        }

        private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

        private fun decode(text: String): ByteArray = Base64.decode(text, Base64.NO_WRAP)

        private fun DataOutputStream.writeSizedString(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            writeInt(bytes.size)
            write(bytes)
        }

        private fun DataOutputStream.writeNullableSizedString(value: String?) {
            if (value == null) {
                writeInt(NULL_VALUE_SIZE)
                return
            }
            writeSizedString(value)
        }

        private fun DataInputStream.readSizedString(): String {
            val size = readInt()
            check(size >= 0) { "Negative string length is invalid." }
            val bytes = ByteArray(size)
            readFully(bytes)
            return bytes.toString(Charsets.UTF_8)
        }

        private fun DataInputStream.readNullableSizedString(): String? {
            val size = readInt()
            if (size == NULL_VALUE_SIZE) {
                return null
            }
            check(size >= 0) { "Negative string length is invalid." }
            val bytes = ByteArray(size)
            readFully(bytes)
            return bytes.toString(Charsets.UTF_8)
        }

        private companion object {
            private const val SESSION_FILE_NAME = "session.preferences_pb"
            private const val FORMAT_VERSION = 1
            private const val NULL_VALUE_SIZE = -1

            private val ENCRYPTED_DATA_KEY = stringPreferencesKey("encrypted_data")
            private val INITIALIZATION_VECTOR_KEY = stringPreferencesKey("initialization_vector")
        }
    }
