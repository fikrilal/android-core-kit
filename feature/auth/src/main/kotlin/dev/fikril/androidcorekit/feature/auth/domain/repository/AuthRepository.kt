package dev.fikril.androidcorekit.feature.auth.domain.repository

import dev.fikril.androidcorekit.core.common.AppResult
import dev.fikril.androidcorekit.feature.auth.domain.model.AuthSession
import dev.fikril.androidcorekit.feature.auth.domain.model.LoginRequest

interface AuthRepository {
    suspend fun login(request: LoginRequest): AppResult<AuthSession>
}
