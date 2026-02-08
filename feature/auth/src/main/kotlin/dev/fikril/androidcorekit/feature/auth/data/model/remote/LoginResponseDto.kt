package dev.fikril.androidcorekit.feature.auth.data.model.remote

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponseDto(
    val user: LoginUserDto,
    val accessToken: String,
    val refreshToken: String,
)
