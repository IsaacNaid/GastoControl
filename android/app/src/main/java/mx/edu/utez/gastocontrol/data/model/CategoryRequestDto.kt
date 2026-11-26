package mx.edu.utez.gastocontrol.data.model

data class CategoryRequestDto(
    val name: String,
    val type: String,
    val color: String = "#2563EB",
    val icon: String = "category"
)
