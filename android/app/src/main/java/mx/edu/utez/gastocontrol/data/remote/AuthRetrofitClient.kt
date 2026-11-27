package mx.edu.utez.gastocontrol.data.remote

import mx.edu.utez.gastocontrol.BuildConfig
import mx.edu.utez.gastocontrol.util.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AuthRetrofitClient {

    private val BASE_URL = BuildConfig.API_BASE_URL

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->

            val request = chain.request()
            val authorization =
                SessionManager.getAuthorization()

            val authenticatedRequest =
                if (
                    authorization != null &&
                    request.header("Authorization") == null
                ) {
                    request.newBuilder()
                        .header("Authorization", authorization)
                        .build()
                } else {
                    request
                }

            chain.proceed(authenticatedRequest)
        }
        .build()

    val retrofit: Retrofit by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val movementApi: MovementApi by lazy {
        retrofit.create(MovementApi::class.java)
    }

    val categoryApi: CategoryApiService by lazy {
        retrofit.create(CategoryApiService::class.java)
    }
}