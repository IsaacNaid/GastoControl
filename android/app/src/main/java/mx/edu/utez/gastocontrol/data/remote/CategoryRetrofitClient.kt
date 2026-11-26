package mx.edu.utez.gastocontrol.data.remote

import mx.edu.utez.gastocontrol.util.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CategoryRetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val authorization = SessionManager.getAuthorization()

            val authenticatedRequest =
                if (authorization != null &&
                    request.header("Authorization") == null) {
                    request.newBuilder()
                        .header("Authorization", authorization)
                        .build()
                } else {
                    request
                }

            chain.proceed(authenticatedRequest)
        }
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: CategoryApiService =
        retrofit.create(CategoryApiService::class.java)
}
