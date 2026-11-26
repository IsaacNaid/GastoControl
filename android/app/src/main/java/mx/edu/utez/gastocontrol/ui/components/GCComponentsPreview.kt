
package mx.edu.utez.gastocontrol.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.edu.utez.gastocontrol.ui.theme.GCIncome
import mx.edu.utez.gastocontrol.ui.theme.GCExpense

@Composable
fun GCComponentsPreview() {

    var nombre by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "GastoControl",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Prueba de componentes oficiales",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        GCSummaryCard(
            title = "Saldo disponible",
            amount = "$750.00"
        )

        GCSummaryCard(
            title = "Ingresos",
            amount = "+$2,000.00",
            amountColor = GCIncome
        )

        GCSummaryCard(
            title = "Gastos",
            amount = "-$1,250.00",
            amountColor = GCExpense
        )

        GCTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = "Nombre de categoría",
            placeholder = "Ej. Alimentación"
        )

        GCPrimaryButton(
            text = "Guardar categoría",
            onClick = {}
        )

        GCOutlinedButton(
            text = "Cancelar",
            onClick = {}
        )
    }
}
