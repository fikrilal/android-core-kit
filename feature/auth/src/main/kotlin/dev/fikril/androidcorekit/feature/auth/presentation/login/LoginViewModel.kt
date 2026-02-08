package dev.fikril.androidcorekit.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fikril.androidcorekit.core.common.AppError
import dev.fikril.androidcorekit.core.common.AppResult
import dev.fikril.androidcorekit.feature.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val loginUseCase: LoginUseCase,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = mutableUiState.asStateFlow()

        fun onEvent(event: LoginUiEvent) {
            when (event) {
                is LoginUiEvent.EmailChanged -> {
                    mutableUiState.update { state ->
                        state.copy(
                            email = event.value,
                            emailError = null,
                            errorMessage = null,
                        )
                    }
                }
                is LoginUiEvent.PasswordChanged -> {
                    mutableUiState.update { state ->
                        state.copy(
                            password = event.value,
                            passwordError = null,
                            errorMessage = null,
                        )
                    }
                }
                LoginUiEvent.SubmitClicked -> submit()
            }
        }

        private fun submit() {
            val state = uiState.value
            if (state.isSubmitting) return

            viewModelScope.launch {
                mutableUiState.update { current ->
                    current.copy(
                        isSubmitting = true,
                        emailError = null,
                        passwordError = null,
                        errorMessage = null,
                    )
                }

                when (
                    val result =
                        loginUseCase(
                            email = state.email,
                            password = state.password,
                        )
                ) {
                    is AppResult.Success -> {
                        mutableUiState.update { current ->
                            current.copy(
                                isSubmitting = false,
                                errorMessage = null,
                            )
                        }
                    }
                    is AppResult.Failure -> applyFailure(result.error)
                }
            }
        }

        private fun applyFailure(error: AppError) {
            val state = uiState.value
            val emailError = (error as? AppError.Validation)?.fieldErrors?.get("email")?.firstOrNull()
            val passwordError = (error as? AppError.Validation)?.fieldErrors?.get("password")?.firstOrNull()

            mutableUiState.value =
                state.copy(
                    isSubmitting = false,
                    emailError = emailError,
                    passwordError = passwordError,
                    errorMessage = error.toUserMessage(),
                )
        }

        private fun AppError.toUserMessage(): String =
            when (this) {
                is AppError.Network -> message ?: "Network request failed."
                AppError.Unauthorized -> "Invalid email or password."
                AppError.NotFound -> "Account not found."
                is AppError.Validation -> message ?: "Please check your input."
                is AppError.Parsing -> message ?: "Unexpected server response."
                is AppError.Server -> message ?: "Server error ($statusCode)."
                is AppError.Unknown -> "Login failed. Please try again."
            }
    }
