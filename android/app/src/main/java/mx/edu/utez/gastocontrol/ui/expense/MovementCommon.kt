package mx.edu.utez.gastocontrol.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// TEMPORAL: pide a Isaac poner estos dos colores en Color.kt y después bórralos de aquí
internal val IncomeColor = Color(0xFF15803D)
internal val ExpenseColor = Color(0xFFDC2626)

fun movementTypeLabel(type: String) = if (type == "INCOME") "Ingreso" else "Gasto"

// Usa BigDecimal: nunca Float ni Double para dinero
fun formatAmount(type: String, amount: String): String {
    val value = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val text = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).format(value)
    val sign = if (type == "INCOME") "+" else "−"
    return "$sign$$text"
}

@Composable
internal fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun MessageView(message: String, buttonText: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(onClick = onClick) { Text(buttonText) }
    }
}