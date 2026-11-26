package mx.edu.utez.gastocontrol.data.remote

import mx.edu.utez.gastocontrol.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<Unit>

    @GET("api/auth/me")
    suspend fun login(
        @Header("Authorization") authorization: String
    ): Response<Unit>
}