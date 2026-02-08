package dev.fikril.androidcorekit.feature.auth.domain.model

data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String? = null,
    val deviceName: String? = null,
)
