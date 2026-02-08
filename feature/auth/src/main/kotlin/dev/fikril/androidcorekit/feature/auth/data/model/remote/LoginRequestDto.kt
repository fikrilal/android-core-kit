package dev.fikril.androidcorekit.feature.auth.data.model.remote

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    val deviceId: String? = null,
    val deviceName: String? = null,
)
