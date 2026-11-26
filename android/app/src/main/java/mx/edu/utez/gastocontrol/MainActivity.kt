package mx.edu.utez.gastocontrol

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import mx.edu.utez.gastocontrol.data.remote.AuthRetrofitClient
import mx.edu.utez.gastocontrol.data.repository.AuthRepository
import mx.edu.utez.gastocontrol.navigation.AppNavigation
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
                AppNavigation(
                    authViewModel = authViewModel,
                    biometricHelper = biometricHelper
                )
            }
        }
    }
}
