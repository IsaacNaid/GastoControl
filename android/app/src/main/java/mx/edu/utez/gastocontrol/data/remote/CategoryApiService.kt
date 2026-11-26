package mx.edu.utez.gastocontrol.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CategoryApiService {

    @GET("api/categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("api/categories/{id}")
    suspend fun getCategoryById(
        @Path("id") id: Long
    ): CategoryDto

    @POST("api/categories")
    suspend fun createCategory(
        @Body request: CategoryRequestDto
    ): CategoryDto

    @PUT("api/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Long,
        @Body request: CategoryRequestDto
    ): CategoryDto

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(
        @Path("id") id: Long
    ): retrofit2.Response<Unit>
}
