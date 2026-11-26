package mx.edu.utez.gastocontrol

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import mx.edu.utez.gastocontrol.data.remote.AuthRetrofitClient
import mx.edu.utez.gastocontrol.data.repository.AuthRepository
import mx.edu.utez.gastocontrol.ui.AppNavigation
import mx.edu.utez.gastocontrol.ui.auth.AuthNavigation
import mx.edu.utez.gastocontrol.ui.auth.AuthViewModel
import mx.edu.utez.gastocontrol.ui.auth.AuthViewModelFactory
import mx.edu.utez.gastocontrol.ui.theme.GastoControlTheme
import mx.edu.utez.gastocontrol.util.BiometricHelper
import mx.edu.utez.gastocontrol.util.SessionManager

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val repository = AuthRepository(
            AuthRetrofitClient.authApi
        )

        val factory = AuthViewModelFactory(repository)

        val authViewModel = ViewModelProvider(
            this,
            factory
        )[AuthViewModel::class.java]

        val biometricHelper = BiometricHelper(this)

        setContent {

            GastoControlTheme {

                var isAuthenticated by remember {
                    mutableStateOf(false)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {

                        if (!isAuthenticated) {

                            AuthNavigation(
                                viewModel = authViewModel,

                                onLoginSuccess = {

                                    if (
                                        biometricHelper
                                            .isBiometricAvailable()
                                    ) {

                                        biometricHelper.authenticate(

                                            onSuccess = {
                                                isAuthenticated = true
                                            },

                                            onError = { message ->

                                                SessionManager.clearSession()
                                                authViewModel.resetForm()

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    message,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        )

                                    } else {
                                        isAuthenticated = true
                                    }
                                }
                            )

                        } else {

                            val sessionOwner = remember {
                                object : ViewModelStoreOwner {
                                    override val viewModelStore =
                                        ViewModelStore()
                                }
                            }

                            DisposableEffect(sessionOwner) {
                                onDispose {
                                    sessionOwner.viewModelStore.clear()
                                }
                            }

                            CompositionLocalProvider(
                                LocalViewModelStoreOwner provides sessionOwner
                            ) {
                                AppNavigation(
                                    onLogout = {

                                        SessionManager.clearSession()

                                        authViewModel.resetForm()

                                        isAuthenticated = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}