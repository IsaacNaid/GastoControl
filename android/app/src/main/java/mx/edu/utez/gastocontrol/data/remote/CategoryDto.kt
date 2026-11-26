package mx.edu.utez.gastocontrol.data.remote

data class CategoryDto(
    val id: Long,
    val name: String,
    val type: String,
    val color: String,
    val icon: String
)

data class CategoryRequestDto(
    val name: String,
    val type: String,
    val color: String = "#2563EB",
    val icon: String = "category"
)
