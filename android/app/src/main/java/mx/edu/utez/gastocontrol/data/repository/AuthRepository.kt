package mx.edu.utez.gastocontrol.data.repository

import mx.edu.utez.gastocontrol.data.model.RegisterRequest
import mx.edu.utez.gastocontrol.data.remote.AuthApi
import mx.edu.utez.gastocontrol.util.SessionManager
import okhttp3.Credentials
import retrofit2.Response

class AuthRepository(
    private val authApi: AuthApi
) {

    suspend fun register(
        name: String,
        email: String,
        password: String
    ): Response<Unit> {

        val request = RegisterRequest(
            name = name.trim(),
            email = email.trim(),
            password = password
        )

        return authApi.register(request)
    }

    suspend fun login(
        email: String,
        password: String
    ): Response<Unit> {

        SessionManager.clearSession()

        val credentials = Credentials.basic(
            email.trim(),
            password
        )

        val response = authApi.login(credentials)

        if (response.isSuccessful) {
            SessionManager.saveSession(credentials)
        }

        return response
    }

    fun logout() {
        SessionManager.clearSession()
    }
}
