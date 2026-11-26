package mx.edu.utez.gastocontrol.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import mx.edu.utez.gastocontrol.data.model.CategoryDto
import mx.edu.utez.gastocontrol.data.remote.AuthRetrofitClient
import mx.edu.utez.gastocontrol.data.repository.MovementRepository
import mx.edu.utez.gastocontrol.ui.dashboard.DashboardViewModel
import mx.edu.utez.gastocontrol.ui.dashboard.HistoryViewModel
import mx.edu.utez.gastocontrol.ui.dashboard.HomeScreen
import mx.edu.utez.gastocontrol.ui.dashboard.HistoryScreen
import mx.edu.utez.gastocontrol.ui.expense.*

@Composable
fun AppNavigation(
    onLogout: () -> Unit
) {

    val repository = remember {
        MovementRepository(
            AuthRetrofitClient.movementApi
        )
    }

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DashboardViewModel(repository)
            }
        }
    )

    val historyViewModel: HistoryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HistoryViewModel(repository)
            }
        }
    )

    val movementViewModel: MovementViewModel = viewModel(
        factory = MovementViewModel.factory(repository)
    )

    var screen by rememberSaveable {
        mutableStateOf("home")
    }

    var previousScreen by rememberSaveable {
        mutableStateOf("home")
    }

    var selectedId by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    var categories by remember {
        mutableStateOf<List<CategoryDto>>(emptyList())
    }

    var categoryError by remember {
        mutableStateOf<String?>(null)
    }

    var loadingCategories by remember {
        mutableStateOf(false)
    }

    var retryCategories by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(screen, retryCategories) {
        if (screen == "create" || screen == "edit") {
            loadingCategories = true
            categoryError = null

            try {
                categories =
                    AuthRetrofitClient.categoryApi.getCategories()
            } catch (e: CancellationException) {
                throw e
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) {
                    onLogout()
                } else {
                    categoryError = "No se pudieron cargar las categorías"
                }
            } catch (e: Exception) {
                categoryError = "Error al consultar categorías"
            } finally {
                loadingCategories = false
            }
        }
    }

    fun goBack() {
        screen = when (screen) {
            "edit" -> "detail"
            "detail", "create" -> previousScreen
            else -> "home"
        }
    }

    BackHandler(enabled = screen != "home") {
        goBack()
    }

    when (screen) {

        "home" -> {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onLogout) {
                        Text("Cerrar sesión")
                    }
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    HomeScreen(
                        viewModel = dashboardViewModel,

                        onNewExpense = {
                            previousScreen = "home"
                            selectedId = null
                            screen = "create"
                        },

                        onOpenHistory = {
                            screen = "history"
                        },

                        onMovementClick = { id ->
                            previousScreen = "home"
                            selectedId = id
                            screen = "detail"
                        }
                    )
                }
            }
        }

        "history" -> {

            HistoryScreen(
                viewModel = historyViewModel,

                onMovementClick = { id ->
                    previousScreen = "history"
                    selectedId = id
                    screen = "detail"
                },

                onBack = {
                    screen = "home"
                }
            )
        }

        "create", "edit" -> {

            if (loadingCategories) {

                CircularProgressIndicator()

            } else if (categoryError != null) {

                Column {
                    Text(categoryError.orEmpty())

                    Button(
                        onClick = { retryCategories++ }
                    ) {
                        Text("Reintentar")
                    }

                    TextButton(onClick = { goBack() }) {
                        Text("Regresar")
                    }
                }

            } else {

                MovementFormScreen(
                    viewModel = movementViewModel,
                    categories = categories,

                    movementId = if (screen == "edit") {
                        selectedId
                    } else {
                        null
                    },

                    onSaved = {
                        screen = previousScreen
                    },

                    onCancel = {
                        goBack()
                    },

                    onUnauthorized = onLogout
                )
            }
        }

        "detail" -> {

            selectedId?.let { id ->

                MovementDetailScreen(
                    viewModel = movementViewModel,
                    movementId = id,

                    onEdit = { movementId ->
                        selectedId = movementId
                        screen = "edit"
                    },

                    onDeleted = {
                        selectedId = null
                        screen = previousScreen
                    },

                    onBack = {
                        goBack()
                    },

                    onUnauthorized = onLogout
                )
            }
        }
    }
}