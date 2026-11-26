package mx.edu.utez.gastocontrol.util

object SessionManager {

    @Volatile
    private var authorization: String? = null

    fun saveSession(credentials: String) {
        authorization = credentials
    }

    fun getAuthorization(): String? {
        return authorization
    }

    fun clearSession() {
        authorization = null
    }
}
