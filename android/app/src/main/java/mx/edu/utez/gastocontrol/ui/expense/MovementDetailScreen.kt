package mx.edu.utez.gastocontrol.ui.expense

import mx.edu.utez.gastocontrol.data.model.MovementDto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MovementDetailScreen(
    viewModel: MovementViewModel,
    movementId: Long,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit = {}
) {
    val state by viewModel.detailState.collectAsState()
    val action by viewModel.actionState.collectAsState()
    val expired by viewModel.sessionExpired.collectAsState()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(movementId) {
        viewModel.resetAction()
        viewModel.loadDetail(movementId)
    }
    LaunchedEffect(action.done) {
        if (action.done) {
            viewModel.resetAction()
            onDeleted()
        }
    }
    LaunchedEffect(expired) {
        if (expired) {
            viewModel.sessionHandled()
            onUnauthorized()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        TextButton(onClick = onBack, enabled = !action.working) { Text("← Regresar") }

        when (val current = state) {
            DetailState.Loading -> LoadingView()
            is DetailState.Error ->
                MessageView(current.message, "Reintentar") { viewModel.loadDetail(movementId) }
            is DetailState.Success ->
                if (current.movement.id == movementId) {
                    DetailContent(
                        movement = current.movement,
                        action = action,
                        onEdit = { onEdit(movementId) },
                        onDelete = { confirmDelete = true }
                    )
                } else {
                    LoadingView()
                }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar movimiento?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(movementId)
                }) { Text("Eliminar", color = ExpenseColor) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun DetailContent(
    movement: MovementDto,
    action: ActionState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = if (movement.type == "INCOME") IncomeColor else ExpenseColor

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${if (movement.type == "INCOME") "↑" else "↓"} ${movementTypeLabel(movement.type)}",
                    color = color,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    formatAmount(movement.type, movement.amount),
                    color = color,
                    style = MaterialTheme.typography.displaySmall
                )
                Text("Fecha: ${movement.date}")
                Text("Concepto: ${movement.description}")
                Text("Categoría: ${movement.categoryName}")
            }
        }

        action.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = onEdit,
            enabled = !action.working,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Editar") }

        OutlinedButton(
            onClick = onDelete,
            enabled = !action.working,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseColor)
        ) { Text(if (action.working) "Eliminando..." else "Eliminar") }
    }
}