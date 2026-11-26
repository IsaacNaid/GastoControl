
package mx.edu.utez.gastocontrol.ui.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.edu.utez.gastocontrol.data.model.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel = viewModel()
) {
    val categories by viewModel.categories.collectAsState()

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var editingCategory by remember {
        mutableStateOf<CategoryEntity?>(null)
    }
    var showDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember {
        mutableStateOf<CategoryEntity?>(null)
    }
    var filter by remember { mutableStateOf("ALL") }

    val blue = Color(0xFF2563EB)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administrar categorías") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                onClick = {
                    editingCategory = null
                    name = ""
                    type = "EXPENSE"
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = blue
                )
            ) {
                Text("Agregar categoría")
            }

            Button(
                onClick = {
                    viewModel.syncCategories()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sincronizar con Oracle")
            }

            Text(
                text = "Filtrar categorías",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Todas",
                    "EXPENSE" to "Gastos",
                    "INCOME" to "Ingresos"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = filter == value,
                        onClick = { filter = value },
                        label = { Text(label) }
                    )
                }
            }

            val filteredCategories = categories.filter {
                filter == "ALL" || it.type == filter
            }

            if (filteredCategories.isEmpty()) {
                Text("No hay categorías para mostrar")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        filteredCategories,
                        key = { it.localId }
                    ) { category ->

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme
                                        .typography.titleMedium
                                )

                                Text(
                                    text = if (
                                        category.type == "EXPENSE"
                                    ) "Gasto" else "Ingreso"
                                )

                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            editingCategory = category
                                            name = category.name
                                            type = category.type
                                            showDialog = true
                                        }
                                    ) {
                                        Text("Editar")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            categoryToDelete = category
                                        }
                                    ) {
                                        Text("Eliminar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    if (editingCategory == null)
                        "Nueva categoría"
                    else
                        "Editar categoría"
                )
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        singleLine = true
                    )

                    Text("Tipo de categoría")

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = type == "EXPENSE",
                            onClick = { type = "EXPENSE" }
                        )
                        Text("Gasto")

                        Spacer(Modifier.width(12.dp))

                        RadioButton(
                            selected = type == "INCOME",
                            onClick = { type = "INCOME" }
                        )
                        Text("Ingreso")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        val current = editingCategory

                        if (current == null) {
                            viewModel.addCategory(
                                name.trim(),
                                type,
                                "#2563EB",
                                "category"
                            )
                        } else {
                            viewModel.updateCategory(
                                current.copy(
                                    name = name.trim(),
                                    type = type,
                                    isSynced = false
                                )
                            )
                        }

                        showDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = {
                categoryToDelete = null
            },
            title = { Text("Eliminar categoría") },
            text = {
                Text(
                    "¿Deseas eliminar '${category.name}'?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category)
                        categoryToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        categoryToDelete = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
