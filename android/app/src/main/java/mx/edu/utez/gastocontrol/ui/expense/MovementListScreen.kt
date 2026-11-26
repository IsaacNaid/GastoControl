package mx.edu.utez.gastocontrol.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MovementListScreen(
    viewModel: MovementViewModel,
    onAddClick: () -> Unit,
    onMovementClick: (Long) -> Unit,
    onUnauthorized: () -> Unit = {}
) {
    val state by viewModel.listState.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val expired by viewModel.sessionExpired.collectAsState()

    // Recarga cada vez que se entra a la pantalla
    LaunchedEffect(Unit) { viewModel.loadMovements() }
    LaunchedEffect(expired) {
        if (expired) {
            viewModel.sessionHandled()
            onUnauthorized()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Mis movimientos", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "Todos", "INCOME" to "Ingresos", "EXPENSE" to "Gastos")
                .forEach { (type, label) ->
                    FilterChip(
                        selected = filter == type,
                        onClick = { viewModel.changeFilter(type) },
                        label = { Text(label) }
                    )
                }
        }

        Button(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Nuevo movimiento") }

        when (val current = state) {
            ListState.Loading -> LoadingView()
            ListState.Empty -> MessageView(
                if (filter == null) "Aún no tienes movimientos"
                else "No hay movimientos de este tipo",
                "Agregar movimiento",
                onAddClick
            )
            is ListState.Error ->
                MessageView(current.message, "Reintentar") { viewModel.loadMovements() }
            is ListState.Success -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(current.movements, key = { it.id }) { movement ->
                    MovementRow(movement, onClick = { onMovementClick(movement.id) })
                }
            }
        }
    }
}