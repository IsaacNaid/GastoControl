package mx.edu.utez.gastocontrol.data.model

// Cuerpo de POST y PUT. No lleva userId: el servidor usa la sesión.
data class MovementRequest(
    val type: String,
    val amount: String,
    val description: String,
    val date: String,
    val categoryId: Long
)