package mx.edu.utez.gastocontrol.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mx.edu.utez.gastocontrol.ui.expense.MovementRow

/**
 * Historial: filtros por tipo y fechas + lista con LazyColumn.
 * Estados: Loading, Empty, Success, Error/Reintentar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onMovementClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filtros by viewModel.filtros.collectAsState()
    val errorFiltro by viewModel.errorFiltro.collectAsState()

    //Recarga al aparecer y al volver del detalle/edición/borrado
    LaunchedEffect(Unit) {
        viewModel.cargarHistorial()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            //Filtro por tipo
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                FilterChip(
                    selected = filtros.tipo == null,
                    onClick = { viewModel.cambiarTipo(null) },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = filtros.tipo == TIPO_INGRESO,
                    onClick = { viewModel.cambiarTipo(TIPO_INGRESO) },
                    label = { Text("Ingresos") }
                )
                FilterChip(
                    selected = filtros.tipo == TIPO_GASTO,
                    onClick = { viewModel.cambiarTipo(TIPO_GASTO) },
                    label = { Text("Gastos") }
                )
            }

            //  Filtro por fechas
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = filtros.desde,
                    onValueChange = viewModel::cambiarDesde,
                    label = { Text("Desde") },
                    placeholder = { Text("AAAA-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = filtros.hasta,
                    onValueChange = viewModel::cambiarHasta,
                    label = { Text("Hasta") },
                    placeholder = { Text("AAAA-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            errorFiltro?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::cargarHistorial) { Text("Aplicar filtros") }
                TextButton(onClick = viewModel::limpiarFiltros) { Text("Limpiar filtros") }
            }

            when (val estado = uiState) {
                HistoryUiState.Loading -> CargandoTemporal()

                is HistoryUiState.Error -> {
                    ErrorConReintento(mensaje = estado.mensaje, onRetry = viewModel::cargarHistorial)
                }

                is HistoryUiState.Empty -> {
                    EtiquetaPeriodo(estado.filtros)
                    ResumenPeriodo(Totales())
                    Text(
                        text = if (hayFiltros(estado.filtros))
                            "No hay movimientos con estos filtros."
                        else
                            "Aún no tienes movimientos",
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }

                is HistoryUiState.Success -> {
                    EtiquetaPeriodo(estado.filtros)
                    ResumenPeriodo(estado.totales)
                    LazyColumn(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(estado.movimientos, key = { it.id }) { movimiento ->
                            MovementRow(
                                movement = movimiento,
                                onClick = { onMovementClick(movimiento.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun hayFiltros(f: FiltrosHistorial): Boolean =
    f.tipo != null || f.desde.isNotEmpty() || f.hasta.isNotEmpty()

/** Indica en pantalla a qué tipo corresponden los totales*/
@Composable
private fun EtiquetaPeriodo(f: FiltrosHistorial) {
    val tipo = when (f.tipo) {
        TIPO_INGRESO -> "Ingresos"
        TIPO_GASTO -> "Gastos"
        else -> "Todos"
    }
    val periodo = when {
        f.desde.isEmpty() && f.hasta.isEmpty() -> "todo el historial"
        f.desde.isEmpty() -> "hasta ${f.hasta}"
        f.hasta.isEmpty() -> "desde ${f.desde}"
        else -> "del ${f.desde} al ${f.hasta}"
    }
    Text("Mostrando: $tipo · $periodo", style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun ResumenPeriodo(totales: Totales) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            FilaTotal("Ingresos del periodo", totales.ingresos)
            FilaTotal("Gastos del periodo", totales.gastos)
            FilaTotal("Saldo del periodo", totales.saldo)
        }
    }
}

@Composable
private fun FilaTotal(etiqueta: String, valor: java.math.BigDecimal) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatoDinero(valor), style = MaterialTheme.typography.bodyMedium)
    }
}

