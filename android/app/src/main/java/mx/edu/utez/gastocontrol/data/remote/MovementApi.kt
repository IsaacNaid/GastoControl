package mx.edu.utez.gastocontrol.data.remote

import mx.edu.utez.gastocontrol.data.model.MovementDto

import mx.edu.utez.gastocontrol.data.model.MovementRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MovementApi {

    // Retrofit omite los parámetros que sean null
    @GET("api/movements")
    suspend fun getMovements(
        @Query("type") type: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): List<MovementDto>

    @GET("api/movements/{id}")
    suspend fun getMovement(@Path("id") id: Long): MovementDto

    @POST("api/movements")
    suspend fun createMovement(@Body request: MovementRequest): MovementDto

    @PUT("api/movements/{id}")
    suspend fun updateMovement(
        @Path("id") id: Long,
        @Body request: MovementRequest
    ): MovementDto

    // DELETE responde 204 sin cuerpo
    @DELETE("api/movements/{id}")
    suspend fun deleteMovement(@Path("id") id: Long): Response<Unit>
}