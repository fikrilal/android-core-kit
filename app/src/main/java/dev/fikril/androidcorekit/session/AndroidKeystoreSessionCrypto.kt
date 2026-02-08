package dev.fikril.androidcorekit.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidKeystoreSessionCrypto
    @Inject
    constructor() : SessionCrypto {
        override fun encrypt(plaintext: ByteArray): EncryptedPayload {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            return EncryptedPayload(
                ciphertext = cipher.doFinal(plaintext),
                iv = cipher.iv,
            )
        }

        override fun decrypt(payload: EncryptedPayload): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), gcmSpec)
            return cipher.doFinal(payload.ciphertext)
        }

        private fun getOrCreateSecretKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existingKey != null) {
                return existingKey
            }

            val keyGenerator =
                KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE,
                )
            val keySpec =
                KeyGenParameterSpec
                    .Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()

            keyGenerator.init(keySpec)
            return keyGenerator.generateKey()
        }

        private companion object {
            private const val ANDROID_KEYSTORE = "AndroidKeyStore"
            private const val TRANSFORMATION = "AES/GCM/NoPadding"
            private const val KEY_ALIAS = "android-core-kit-session-key"
            private const val KEY_SIZE_BITS = 256
            private const val GCM_TAG_LENGTH_BITS = 128
        }
    }
