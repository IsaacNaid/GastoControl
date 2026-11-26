package mx.edu.utez.gastocontrol.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.math.BigDecimal
import mx.edu.utez.gastocontrol.data.model.MovementDto
import mx.edu.utez.gastocontrol.sensor.ShakeDetector
import mx.edu.utez.gastocontrol.ui.expense.MovementRow

private val MargenPantalla = 20.dp
private val SeparacionBloques = 16.dp
private val FormaTarjeta = RoundedCornerShape(16.dp)

/**
 * Inicio / Dashboard: periodo · Saldo · Ingresos y Gastos ·
 * Gastos por categoría con barras · Movimientos recientes.
 * @param onNewExpense     lo dispara el acelerómetro: abrir «Nuevo gasto»
 * @param onOpenHistory    «Ver historial» en la sección de recientes
 * @param onMovementClick  tocar un movimiento reciente abre su detalle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    onNewExpense: () -> Unit,
    onOpenHistory: () -> Unit,
    onMovementClick: (Long) -> Unit,
    // Botón normal
    // Si nadie lo pasa, usa la misma acción que el sensor.
    onNewMovement: () -> Unit = onNewExpense,
    // Se llama si el servidor responde 401 (sesión no válida) → volver al login.
    // Tiene valor por defecto para no romper la navegación actual.
    onUnauthorized: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Recarga cada vez que la pantalla aparece (también al volver de crear/editar/borrar)
    LaunchedEffect(Unit) {
        viewModel.cargarResumen()
    }

    // ---------- Acelerómetro ligado al ciclo de vida ----------
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnNewExpense by rememberUpdatedState(onNewExpense)

    val shakeDetector = remember(context) {
        ShakeDetector(context) { currentOnNewExpense() }
    }

    DisposableEffect(lifecycleOwner, shakeDetector) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> shakeDetector.start()
                Lifecycle.Event.ON_PAUSE -> shakeDetector.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Si la pantalla aparece cuando la Activity YA está en RESUMED
        // (por ejemplo, al regresar del formulario), se inicia de inmediato.
        // start() ignora la llamada si ya estaba escuchando.
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            shakeDetector.start()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            shakeDetector.stop()
        }
    }

    // ---------- Sesión no válida (401) → volver al login ----------
    val currentOnUnauthorized by rememberUpdatedState(onUnauthorized)
    LaunchedEffect(uiState) {
        val estado = uiState
        if (estado is DashboardUiState.Error && estado.sesionInvalida) {
            currentOnUnauthorized()
        }
    }

    // ---------- Interfaz ----------
    Scaffold(
        topBar = { TopAppBar(title = { Text("GastoControl") }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (val estado = uiState) {
            DashboardUiState.Loading -> CargandoTemporal(Modifier.padding(innerPadding))

            is DashboardUiState.Error -> ErrorConReintento(
                mensaje = estado.mensaje,
                onRetry = viewModel::cargarResumen,
                modifier = Modifier.padding(innerPadding)
            )

            is DashboardUiState.Empty -> ContenidoDashboard(
                periodo = estado.periodo,
                totales = Totales(),
                gastosPorCategoria = emptyList(),
                recientes = emptyList(),
                mostrarTipSensor = shakeDetector.isAvailable,
                onOpenHistory = onOpenHistory,
                onMovementClick = onMovementClick,
                onNewMovement = onNewMovement,
                modifier = Modifier.padding(innerPadding)
            )

            is DashboardUiState.Success -> ContenidoDashboard(
                periodo = estado.periodo,
                totales = estado.totales,
                gastosPorCategoria = estado.gastosPorCategoria,
                recientes = estado.recientes,
                mostrarTipSensor = shakeDetector.isAvailable,
                onOpenHistory = onOpenHistory,
                onMovementClick = onMovementClick,
                onNewMovement = onNewMovement,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun ContenidoDashboard(
    periodo: Periodo,
    totales: Totales,
    gastosPorCategoria: List<GastoCategoria>,
    recientes: List<MovementDto>,
    mostrarTipSensor: Boolean,
    onOpenHistory: () -> Unit,
    onMovementClick: (Long) -> Unit,
    onNewMovement: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MargenPantalla, vertical = SeparacionBloques),
        verticalArrangement = Arrangement.spacedBy(SeparacionBloques)
    ) {
        // 1. Periodo
        item {
            Text(
                "Periodo: ${periodo.etiqueta}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2. Tarjeta grande de Saldo
        item { TarjetaSaldo(totales.saldo) }

        // 3. Ingresos (verde) y Gastos (rojo) lado a lado
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(SeparacionBloques)) {
                TarjetaMonto(
                    titulo = "Ingresos",
                    valor = totales.ingresos,
                    colorFondo = MaterialTheme.colorScheme.tertiaryContainer,
                    colorMonto = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                TarjetaMonto(
                    titulo = "Gastos",
                    valor = totales.gastos,
                    colorFondo = MaterialTheme.colorScheme.errorContainer,
                    colorMonto = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Acción principal (TEMPORAL → GCPrimaryButton)
        item {
            Button(onClick = onNewMovement, modifier = Modifier.fillMaxWidth()) {
                Text("Nuevo movimiento")
            }
        }

        // 4. Gastos por categoría con barras de progreso
        item {
            Text("Gastos por categoría", style = MaterialTheme.typography.titleMedium)
        }
        item {
            if (gastosPorCategoria.isEmpty()) {
                Text(
                    "Sin gastos en este periodo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TarjetaGastosPorCategoria(gastosPorCategoria)
            }
        }

        // 5. Movimientos recientes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Movimientos recientes", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onOpenHistory) { Text("Ver historial") }
            }
        }

        if (recientes.isEmpty()) {
            item {
                Text(
                    "Aún no tienes movimientos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(recientes, key = { it.id }) { movimiento ->
                MovementRow(movement = movimiento, onClick = { onMovementClick(movimiento.id) })
            }
        }

        // Ayuda del sensor solo si el equipo tiene acelerómetro
        if (mostrarTipSensor) {
            item {
                Text(
                    "Tip: agita el celular para registrar un gasto rápido.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun TarjetaSaldo(saldo: BigDecimal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Saldo",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatoDinero(saldo),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (saldo.signum() < 0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TarjetaMonto(
    titulo: String,
    valor: BigDecimal,
    colorFondo: Color,
    colorMonto: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = FormaTarjeta,
        colors = CardDefaults.cardColors(containerColor = colorFondo)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelLarge)
            Text(
                formatoDinero(valor),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorMonto
            )
        }
    }
}

@Composable
private fun TarjetaGastosPorCategoria(categorias: List<GastoCategoria>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categorias.forEach { categoria ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(categoria.nombre, style = MaterialTheme.typography.bodyMedium)
                        Text(formatoDinero(categoria.total), style = MaterialTheme.typography.bodyMedium)
                    }
                    LinearProgressIndicator(
                        progress = { categoria.proporcion },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.errorContainer
                    )
                }
            }
        }
    }
}



@Composable
internal fun CargandoTemporal(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Mensaje entendible + Reintentar (estado Error). Lo usan Inicio e Historial. */
@Composable
internal fun ErrorConReintento(
    mensaje: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MargenPantalla, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SeparacionBloques)
    ) {
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

