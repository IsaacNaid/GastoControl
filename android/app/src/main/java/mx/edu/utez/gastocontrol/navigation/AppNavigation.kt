
package mx.edu.utez.gastocontrol.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mx.edu.utez.gastocontrol.ui.category.CategoryScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
    const val CATEGORIES = "categories"
    const val EXPENSES = "expenses"
    const val HISTORY = "history"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.LOGIN) {
            TemporaryScreen("Iniciar sesión", navController)
        }

        composable(Routes.REGISTER) {
            TemporaryScreen("Registro", navController)
        }

        composable(Routes.DASHBOARD) {
            TemporaryScreen("Inicio", navController)
        }

        composable(Routes.CATEGORIES) {
            CategoryScreen()
        }

        composable(Routes.EXPENSES) {
            TemporaryScreen("Movimientos", navController)
        }

        composable(Routes.HISTORY) {
            TemporaryScreen("Historial", navController)
        }
    }
}

@Composable
private fun TemporaryScreen(
    title: String,
    navController: NavHostController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "GastoControl",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Pantalla pendiente de integración",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = {
                navController.navigate(Routes.CATEGORIES)
            }
        ) {
            Text("Administrar categorías")
        }

        Button(
            onClick = {
                navController.navigate(Routes.DASHBOARD)
            }
        ) {
            Text("Ir al inicio")
        }
    }
}
