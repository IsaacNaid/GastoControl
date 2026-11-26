package mx.edu.utez.gastocontrol.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun AuthNavigation(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {

    var currentScreen by rememberSaveable {
        mutableStateOf("login")
    }

    val state by viewModel.uiState.collectAsState()

    BackHandler(
        enabled = currentScreen == "register"
    ) {
        if (!state.isLoading) {
            viewModel.resetForm()
            currentScreen = "login"
        }
    }

    when (currentScreen) {

        "login" -> {

            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = {
                    currentScreen = "register"
                },
                onLoginSuccess = {
                    onLoginSuccess()
                }
            )
        }

        "register" -> {

            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    currentScreen = "login"
                }
            )
        }
    }
}