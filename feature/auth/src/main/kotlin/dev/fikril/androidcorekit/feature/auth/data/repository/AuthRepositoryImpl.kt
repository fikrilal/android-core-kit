package dev.fikril.androidcorekit.feature.auth.data.repository

import dev.fikril.androidcorekit.core.common.AppError
import dev.fikril.androidcorekit.core.common.AppResult
import dev.fikril.androidcorekit.core.network.error.toAppError
import dev.fikril.androidcorekit.feature.auth.data.mapper.toDomain
import dev.fikril.androidcorekit.feature.auth.data.mapper.toDto
import dev.fikril.androidcorekit.feature.auth.data.remote.AuthRemoteDataSource
import dev.fikril.androidcorekit.feature.auth.domain.model.AuthSession
import dev.fikril.androidcorekit.feature.auth.domain.model.LoginRequest
import dev.fikril.androidcorekit.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: AuthRemoteDataSource,
    ) : AuthRepository {
        override suspend fun login(request: LoginRequest): AppResult<AuthSession> {
            val response = remoteDataSource.login(request.toDto())
            if (response.isError) {
                return AppResult.Failure(response.toAppError())
            }

            val payload = response.data
            return if (payload == null) {
                AppResult.Failure(
                    AppError.Parsing(
                        message = "Missing authentication payload.",
                        code = response.code,
                    ),
                )
            } else {
                AppResult.Success(payload.toDomain())
            }
        }
    }
