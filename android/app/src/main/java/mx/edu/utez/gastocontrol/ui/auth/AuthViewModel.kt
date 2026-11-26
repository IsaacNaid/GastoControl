package mx.edu.utez.gastocontrol.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import mx.edu.utez.gastocontrol.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(name = value, errorMessage = null)
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(email = value, errorMessage = null)
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(password = value, errorMessage = null)
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                errorMessage = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update {
            it.copy(passwordVisible = !it.passwordVisible)
        }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update {
            it.copy(
                confirmPasswordVisible =
                    !it.confirmPasswordVisible
            )
        }
    }

    fun login() {

        val state = _uiState.value

        if (state.isLoading) return

        if (state.email.isBlank() ||
            state.password.isBlank()
        ) {
            showError("Completa todos los campos")
            return
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(state.email.trim()).matches()
        ) {
            showError("Ingresa un correo válido")
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
        }

        viewModelScope.launch {

            try {

                val response = repository.login(
                    email = state.email,
                    password = state.password
                )

                if (response.isSuccessful) {

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            password = "",
                            confirmPassword = "",
                            passwordVisible = false,
                            successMessage = "Acceso exitoso"
                        )
                    }

                } else {

                    when (response.code()) {
                        401 -> showError(
                            "Correo o contraseña incorrectos"
                        )

                        403 -> showError(
                            "Acceso no autorizado"
                        )

                        else -> showError(
                            "No se pudo iniciar sesión"
                        )
                    }
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: IOException) {
                showError("Error de conexión con el servidor")

            } catch (e: Exception) {
                showError("Ocurrió un error inesperado")
            }
        }
    }

    fun register() {

        val state = _uiState.value

        if (state.isLoading) return

        if (state.name.isBlank() ||
            state.email.isBlank() ||
            state.password.isBlank() ||
            state.confirmPassword.isBlank()
        ) {
            showError("Completa todos los campos")
            return
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(state.email.trim()).matches()
        ) {
            showError("Ingresa un correo válido")
            return
        }

        if (state.password.length < 8) {
            showError(
                "La contraseña debe tener al menos 8 caracteres"
            )
            return
        }

        if (state.password != state.confirmPassword) {
            showError("Las contraseñas no coinciden")
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
        }

        viewModelScope.launch {

            try {

                val response = repository.register(
                    name = state.name,
                    email = state.email,
                    password = state.password
                )

                if (response.isSuccessful) {

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegistered = true,
                            password = "",
                            confirmPassword = "",
                            passwordVisible = false,
                            confirmPasswordVisible = false,
                            successMessage =
                                "Cuenta registrada correctamente"
                        )
                    }

                } else {

                    when (response.code()) {

                        400 -> showError(
                            "Verifica los datos ingresados"
                        )

                        409 -> showError(
                            "El correo ya está registrado"
                        )

                        else -> showError(
                            "No se pudo registrar la cuenta"
                        )
                    }
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: IOException) {
                showError("Error de conexión con el servidor")

            } catch (e: Exception) {
                showError("Ocurrió un error inesperado")
            }
        }
    }

    private fun showError(message: String) {

        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = message,
                successMessage = null
            )
        }
    }

    fun clearMessages() {

        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun resetForm() {
        if (!_uiState.value.isLoading) {
            _uiState.value = AuthUiState()
        }
    }
}