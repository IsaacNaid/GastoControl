package mx.edu.utez.gastocontrol.data.model

data class MovementDto(
    val id: Long,
    val type: String,
    val amount: String,
    val description: String,
    val date: String,
    val categoryId: Long,
    val categoryName: String
)
