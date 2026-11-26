package mx.edu.utez.gastocontrol.data.remote

import mx.edu.utez.gastocontrol.data.model.CategoryDto
import retrofit2.http.GET

interface CategoryApi {

    @GET("api/categories")
    suspend fun getCategories(): List<CategoryDto>
}