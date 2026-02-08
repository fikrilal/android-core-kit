package dev.fikril.androidcorekit.feature.auth.domain.model

data class AuthSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
)
