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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.edu.utez.gastocontrol.data.remote.AuthRetrofitClient
import mx.edu.utez.gastocontrol.data.repository.AuthRepository
import mx.edu.utez.gastocontrol.ui.auth.AuthNavigation
import mx.edu.utez.gastocontrol.ui.auth.AuthViewModel
import mx.edu.utez.gastocontrol.ui.auth.AuthViewModelFactory
import mx.edu.utez.gastocontrol.ui.theme.GastoControlTheme
import mx.edu.utez.gastocontrol.util.BiometricHelper

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

                                                authViewModel.onBiometricError(message)

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

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.Center
                            ) {

                                Text(
                                    text = "Bienvenido a GastoControl",
                                    style = MaterialTheme
                                        .typography.headlineSmall
                                )

                                Spacer(
                                    modifier = Modifier.height(16.dp)
                                )

                                Button(
                                    onClick = {
                                        authViewModel.logout()
                                        isAuthenticated = false
                                    }
                                ) {
                                    Text("Cerrar sesión")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}