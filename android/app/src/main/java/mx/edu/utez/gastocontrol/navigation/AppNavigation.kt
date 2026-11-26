package mx.edu.utez.gastocontrol.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.CancellationException
import mx.edu.utez.gastocontrol.data.model.CategoryDto
import mx.edu.utez.gastocontrol.data.remote.AuthRetrofitClient
import mx.edu.utez.gastocontrol.data.repository.MovementRepository
import mx.edu.utez.gastocontrol.ui.auth.AuthViewModel
import mx.edu.utez.gastocontrol.ui.auth.LoginScreen
import mx.edu.utez.gastocontrol.ui.auth.RegisterScreen
import mx.edu.utez.gastocontrol.ui.category.CategoryScreen
import mx.edu.utez.gastocontrol.ui.dashboard.DashboardViewModel
import mx.edu.utez.gastocontrol.ui.dashboard.HistoryViewModel
import mx.edu.utez.gastocontrol.ui.dashboard.HistoryScreen
import mx.edu.utez.gastocontrol.ui.dashboard.HomeScreen
import mx.edu.utez.gastocontrol.ui.expense.MovementDetailScreen
import mx.edu.utez.gastocontrol.ui.expense.MovementFormScreen
import mx.edu.utez.gastocontrol.ui.expense.MovementListScreen
import mx.edu.utez.gastocontrol.ui.expense.MovementViewModel
import mx.edu.utez.gastocontrol.util.BiometricHelper
import mx.edu.utez.gastocontrol.util.SessionManager
import retrofit2.HttpException

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
    const val CATEGORIES = "categories"
    const val EXPENSES = "expenses"
    const val HISTORY = "history"
    const val CREATE = "movement/create"
    const val DETAIL = "movement/detail/{id}"
    const val EDIT = "movement/edit/{id}"

    fun detail(id: Long) = "movement/detail/$id"
    fun edit(id: Long) = "movement/edit/$id"
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    biometricHelper: BiometricHelper
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    var authenticated by remember { mutableStateOf(false) }
    var biometricPending by remember { mutableStateOf(false) }
    val repository = remember { MovementRepository(AuthRetrofitClient.movementApi) }

    fun logout() {
        authenticated = false
        biometricPending = false
        authViewModel.logout()
        navController.navigate(Routes.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun openDashboard() {
        if (SessionManager.getAuthorization() == null) {
            logout()
            return
        }
        authenticated = true
        navController.navigate(Routes.DASHBOARD) {
            popUpTo(Routes.LOGIN) { inclusive = true }
            launchSingleTop = true
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                    onLoginSuccess = {
                        if (!biometricPending && !authenticated) {
                            if (biometricHelper.isBiometricAvailable()) {
                                biometricPending = true
                                biometricHelper.authenticate(
                                    onSuccess = {
                                        biometricPending = false
                                        openDashboard()
                                    },
                                    onError = { message ->
                                        biometricPending = false
                                        authViewModel.onBiometricError(message)
                                    }
                                )
                            } else {
                                openDashboard()
                            }
                        }
                    }
                )
            }
            composable(Routes.REGISTER) {
                BackHandler {
                    if (!authState.isLoading) {
                        authViewModel.resetForm()
                        navController.popBackStack()
                    }
                }
                RegisterScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Routes.DASHBOARD) {
                ProtectedScreen(authenticated, { logout() }) {
                    val dashboardViewModel: DashboardViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { DashboardViewModel(repository) }
                        }
                    )
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { navController.navigate(Routes.CATEGORIES) }) {
                                Text("Categorías")
                            }
                            TextButton(onClick = { logout() }) { Text("Cerrar sesión") }
                        }
                        TextButton(onClick = { navController.navigate(Routes.EXPENSES) }) {
                            Text("Mis movimientos")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            HomeScreen(
                                viewModel = dashboardViewModel,
                                onUnauthorized = { logout() },
                                onNewExpense = { navController.navigate(Routes.CREATE) },
                                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                                onMovementClick = { id -> navController.navigate(Routes.detail(id)) }
                            )
                        }
                    }
                }
            }
            composable(Routes.HISTORY) {
                ProtectedScreen(authenticated, { logout() }) {
                    val historyViewModel: HistoryViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { HistoryViewModel(repository) }
                        }
                    )
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onUnauthorized = { logout() },
                        onMovementClick = { id -> navController.navigate(Routes.detail(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Routes.CATEGORIES) {
                ProtectedScreen(authenticated, { logout() }) {
                    CategoryScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(Routes.EXPENSES) {
                ProtectedScreen(authenticated, { logout() }) {
                    val movementViewModel: MovementViewModel = viewModel(
                        factory = MovementViewModel.factory(repository)
                    )
                    Column(modifier = Modifier.fillMaxSize()) {
                        TextButton(onClick = { navController.popBackStack() }) { Text("Volver") }
                        Box(modifier = Modifier.weight(1f)) {
                            MovementListScreen(
                                viewModel = movementViewModel,
                                onAddClick = { navController.navigate(Routes.CREATE) },
                                onMovementClick = { id -> navController.navigate(Routes.detail(id)) },
                                onUnauthorized = { logout() }
                            )
                        }
                    }
                }
            }
            composable(Routes.CREATE) {
                ProtectedScreen(authenticated, { logout() }) {
                    val movementViewModel: MovementViewModel = viewModel(
                        factory = MovementViewModel.factory(repository)
                    )
                    MovementEditor(
                        viewModel = movementViewModel,
                        movementId = null,
                        biometricHelper = biometricHelper,
                        onSaved = { navController.popBackStack() },
                        onCancel = { navController.popBackStack() },
                        onUnauthorized = { logout() }
                    )
                }
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                ProtectedScreen(authenticated, { logout() }) {
                    val id = requireNotNull(entry.arguments).getLong("id")
                    val movementViewModel: MovementViewModel = viewModel(
                        factory = MovementViewModel.factory(repository)
                    )
                    MovementDetailScreen(
                        viewModel = movementViewModel,
                        movementId = id,
                        onEdit = { movementId -> navController.navigate(Routes.edit(movementId)) },
                        onDeleted = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                        onUnauthorized = { logout() }
                    )
                }
            }
            composable(Routes.EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                ProtectedScreen(authenticated, { logout() }) {
                    val id = requireNotNull(entry.arguments).getLong("id")
                    val movementViewModel: MovementViewModel = viewModel(
                        factory = MovementViewModel.factory(repository)
                    )
                    MovementEditor(
                        viewModel = movementViewModel,
                        movementId = id,
                        biometricHelper = biometricHelper,
                        onSaved = { navController.popBackStack(Routes.DETAIL, inclusive = true) },
                        onCancel = { navController.popBackStack() },
                        onUnauthorized = { logout() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProtectedScreen(
    authenticated: Boolean,
    onUnauthorized: () -> Unit,
    content: @Composable () -> Unit
) {
    if (authenticated && SessionManager.getAuthorization() != null) {
        content()
    } else {
        LaunchedEffect(Unit) { onUnauthorized() }
    }
}

@Composable
private fun MovementEditor(
    viewModel: MovementViewModel,
    movementId: Long?,
    biometricHelper: BiometricHelper,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    onUnauthorized: () -> Unit
) {
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf(0) }
    var biometricPending by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    var active by remember { mutableStateOf(true) }
    var pendingConfirmation by remember { mutableStateOf<(() -> Unit)?>(null) }

    DisposableEffect(Unit) {
        onDispose { active = false }
    }

    LaunchedEffect(retry) {
        loading = true
        error = null
        try {
            categories = AuthRetrofitClient.categoryApi.getCategories()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401) onUnauthorized()
            else error = "No se pudieron cargar las categorías"
        } catch (e: Exception) {
            error = "Error al consultar categorías"
        } finally {
            loading = false
        }
    }

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(error.orEmpty())
            Button(onClick = { retry++ }) { Text("Reintentar") }
            TextButton(onClick = onCancel) { Text("Regresar") }
        }
        else -> MovementFormScreen(
            viewModel = viewModel,
            categories = categories,
            movementId = movementId,
            onSaved = onSaved,
            onCancel = onCancel,
            onUnauthorized = onUnauthorized,
            confirmSave = { onConfirmed ->
                if (!biometricPending && pendingConfirmation == null) {
                    if (biometricHelper.isBiometricAvailable()) {
                        biometricPending = true
                        biometricHelper.authenticate(
                            onSuccess = {
                                biometricPending = false
                                if (active && SessionManager.getAuthorization() != null) {
                                    onConfirmed()
                                }
                            },
                            onError = { message ->
                                biometricPending = false
                                if (active) biometricError = message
                            }
                        )
                    } else {
                        pendingConfirmation = onConfirmed
                    }
                }
            }
        )
    }

    pendingConfirmation?.let { confirm ->
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = { Text("Confirmar movimiento") },
            text = { Text("¿Deseas guardar este movimiento?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingConfirmation = null
                    if (active && SessionManager.getAuthorization() != null) confirm()
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmation = null }) { Text("Cancelar") }
            }
        )
    }

    biometricError?.let { message ->
        AlertDialog(
            onDismissRequest = { biometricError = null },
            title = { Text("Confirmación cancelada") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { biometricError = null }) { Text("Aceptar") }
            }
        )
    }
}
