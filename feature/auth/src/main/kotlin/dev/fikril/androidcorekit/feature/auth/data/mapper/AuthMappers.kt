package dev.fikril.androidcorekit.feature.auth.data.mapper

import dev.fikril.androidcorekit.feature.auth.data.model.remote.LoginRequestDto
import dev.fikril.androidcorekit.feature.auth.data.model.remote.LoginResponseDto
import dev.fikril.androidcorekit.feature.auth.domain.model.AuthSession
import dev.fikril.androidcorekit.feature.auth.domain.model.LoginRequest

internal fun LoginRequest.toDto(): LoginRequestDto =
    LoginRequestDto(
        email = email,
        password = password,
        deviceId = deviceId,
        deviceName = deviceName,
    )

internal fun LoginResponseDto.toDomain(): AuthSession =
    AuthSession(
        userId = user.id,
        accessToken = accessToken,
        refreshToken = refreshToken,
    )
