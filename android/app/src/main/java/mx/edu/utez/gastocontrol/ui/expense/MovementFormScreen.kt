package mx.edu.utez.gastocontrol.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mx.edu.utez.gastocontrol.data.model.CategoryDto
import mx.edu.utez.gastocontrol.data.model.MovementDto
import mx.edu.utez.gastocontrol.data.model.MovementRequest
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * movementId == null: nuevo. Con valor: editar.
 * categories: las entrega el módulo de categorías de Isaac.
 * confirmSave: aquí Miriam conecta la biometría; por defecto guarda directo.
 */
@Composable
fun MovementFormScreen(
    viewModel: MovementViewModel,
    categories: List<CategoryDto>,
    movementId: Long? = null,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    onUnauthorized: () -> Unit = {},
    confirmSave: (onConfirmed: () -> Unit) -> Unit = { it() }
) {
    val action by viewModel.actionState.collectAsState()
    val detail by viewModel.detailState.collectAsState()
    val expired by viewModel.sessionExpired.collectAsState()

    LaunchedEffect(movementId) {
        viewModel.resetAction()
        // No recargar si ya está cargado: así al girar no se pierde lo escrito
        val loaded = (detail as? DetailState.Success)?.movement?.id
        if (movementId != null && loaded != movementId) viewModel.loadDetail(movementId)
    }
    LaunchedEffect(action.done) {
        if (action.done) {
            viewModel.resetAction()
            onSaved()
        }
    }
    LaunchedEffect(expired) {
        if (expired) {
            viewModel.sessionHandled()
            onUnauthorized()
        }
    }

    if (movementId == null) {
        MovementForm(null, categories, action, onCancel) { request ->
            confirmSave { viewModel.save(null, request) }
        }
    } else {
        val current = detail
        when {
            current is DetailState.Success && current.movement.id == movementId ->
                MovementForm(current.movement, categories, action, onCancel) { request ->
                    confirmSave { viewModel.save(movementId, request) }
                }
            current is DetailState.Error -> MessageView(current.message, "Reintentar") {
                viewModel.loadDetail(movementId)
            }
            else -> LoadingView()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovementForm(
    initial: MovementDto?,
    categories: List<CategoryDto>,
    action: ActionState,
    onCancel: () -> Unit,
    onSubmit: (MovementRequest) -> Unit
) {
    // "Nuevo gasto" es el valor inicial (también sirve para el acelerómetro)
    var type by rememberSaveable { mutableStateOf(initial?.type ?: "EXPENSE") }
    var amount by rememberSaveable { mutableStateOf(initial?.amount ?: "") }
    var description by rememberSaveable { mutableStateOf(initial?.description ?: "") }
    var date by rememberSaveable { mutableStateOf(initial?.date ?: "") }
    var categoryId by rememberSaveable { mutableStateOf(initial?.categoryId) }

    var submitted by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Hasta 10 enteros y 2 decimales, mayor que cero
    val cleanAmount = amount.trim()
    val validAmount = cleanAmount.matches(Regex("""\d{1,10}(\.\d{1,2})?""")) &&
            cleanAmount.toBigDecimal().signum() > 0
    val filteredCategories = categories.filter { it.type == type }
    val selected = filteredCategories.find { it.id == categoryId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (initial == null) "Nuevo movimiento" else "Editar movimiento",
            style = MaterialTheme.typography.headlineSmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("EXPENSE", "INCOME").forEach { option ->
                FilterChip(
                    selected = type == option,
                    onClick = {
                        if (type != option) {
                            type = option
                            categoryId = null
                            menuOpen = false
                        }
                    },
                    label = { Text(movementTypeLabel(option)) }
                )
            }
        }

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.replace(',', '.') },
            label = { Text("Monto") },
            prefix = { Text("$") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = submitted && !validAmount,
            supportingText = {
                if (submitted && !validAmount) {
                    Text("Escribe un monto mayor que cero, máximo 2 decimales")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Concepto") },
            singleLine = true,
            isError = submitted && description.isBlank(),
            supportingText = {
                if (submitted && description.isBlank()) Text("El concepto es obligatorio")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Column {
            Box {
                OutlinedButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(selected?.name ?: "Selecciona una categoría") }

                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    filteredCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                categoryId = category.id
                                menuOpen = false
                            }
                        )
                    }
                }
            }

            if (filteredCategories.isEmpty()) {
                FieldError("No hay categorías disponibles. Crea una primero.")
            } else if (submitted && selected == null) {
                FieldError("La categoría es obligatoria")
            }
        }

        Column {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (date.isEmpty()) "Selecciona la fecha" else "Fecha: $date") }
            if (submitted && date.isEmpty()) FieldError("La fecha es obligatoria")
        }

        action.error?.let { FieldError(it) }

        Button(
            onClick = {
                submitted = true
                val category = selected
                if (validAmount && description.isNotBlank() &&
                    category != null && date.isNotEmpty()
                ) {
                    onSubmit(
                        MovementRequest(
                            type = type,
                            amount = cleanAmount.toBigDecimal()
                                .setScale(2, RoundingMode.UNNECESSARY)
                                .toPlainString(),
                            description = description.trim(),
                            date = date,
                            categoryId = category.id
                        )
                    )
                }
            },
            enabled = !action.working,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text(if (action.working) "Guardando..." else "Guardar movimiento") }

        OutlinedButton(
            onClick = onCancel,
            enabled = !action.working,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Cancelar") }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = toIsoDate(it) }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun FieldError(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
    )
}

// El DatePicker entrega milisegundos en UTC; usar UTC evita restar un día
private fun toIsoDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(millis))
}