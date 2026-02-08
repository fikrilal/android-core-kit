package dev.fikril.androidcorekit.feature.auth.domain.usecase

import dev.fikril.androidcorekit.core.common.AppError
import dev.fikril.androidcorekit.core.common.AppResult
import dev.fikril.androidcorekit.core.session.SessionManager
import dev.fikril.androidcorekit.core.session.SessionTokens
import dev.fikril.androidcorekit.feature.auth.domain.model.LoginRequest
import dev.fikril.androidcorekit.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val sessionManager: SessionManager,
    ) {
        suspend operator fun invoke(
            email: String,
            password: String,
        ): AppResult<Unit> {
            validate(email = email, password = password)?.let { validationError ->
                return AppResult.Failure(validationError)
            }

            val result =
                authRepository.login(
                    request =
                        LoginRequest(
                            email = email.trim(),
                            password = password,
                        ),
                )

            return when (result) {
                is AppResult.Success -> {
                    val session = result.value
                    sessionManager.setTokens(
                        SessionTokens(
                            accessToken = session.accessToken,
                            refreshToken = session.refreshToken,
                        ),
                    )
                    sessionManager.setAuthenticated(userId = session.userId)
                    AppResult.Success(Unit)
                }
                is AppResult.Failure -> result
            }
        }

        private fun validate(
            email: String,
            password: String,
        ): AppError.Validation? {
            val fieldErrors = mutableMapOf<String, MutableList<String>>()

            when {
                email.isBlank() -> fieldErrors.add("email", "Email is required.")
                !EMAIL_REGEX.matches(email.trim()) -> fieldErrors.add("email", "Email format is invalid.")
            }

            if (password.isBlank()) {
                fieldErrors.add("password", "Password is required.")
            }

            if (fieldErrors.isEmpty()) return null

            return AppError.Validation(
                message = "Please fix the highlighted fields.",
                fieldErrors = fieldErrors.mapValues { entry -> entry.value.toList() },
                code = "LOGIN_VALIDATION_FAILED",
            )
        }

        private fun MutableMap<String, MutableList<String>>.add(
            key: String,
            message: String,
        ) {
            getOrPut(key) { mutableListOf() }.add(message)
        }

        private companion object {
            private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        }
    }
