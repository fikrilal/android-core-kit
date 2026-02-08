package dev.fikril.androidcorekit.session

data class EncryptedPayload(
    val ciphertext: ByteArray,
    val iv: ByteArray,
)

interface SessionCrypto {
    fun encrypt(plaintext: ByteArray): EncryptedPayload

    fun decrypt(payload: EncryptedPayload): ByteArray
}
