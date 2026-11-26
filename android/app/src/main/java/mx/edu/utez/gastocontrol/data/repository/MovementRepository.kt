package mx.edu.utez.gastocontrol.data.repository

import mx.edu.utez.gastocontrol.data.model.MovementDto

import kotlinx.coroutines.CancellationException
import mx.edu.utez.gastocontrol.data.model.MovementRequest
import mx.edu.utez.gastocontrol.data.remote.MovementApi
import retrofit2.HttpException
import java.io.IOException

class ApiException(val code: Int?, message: String) : Exception(message)

// Brian reutiliza este repositorio en el dashboard y el historial
class MovementRepository(private val api: MovementApi) {

    suspend fun getMovements(
        type: String? = null,
        from: String? = null,
        to: String? = null
    ): Result<List<MovementDto>> = safeCall { api.getMovements(type, from, to) }

    suspend fun getMovement(id: Long): Result<MovementDto> =
        safeCall { api.getMovement(id) }

    suspend fun createMovement(request: MovementRequest): Result<MovementDto> =
        safeCall { api.createMovement(request) }

    suspend fun updateMovement(id: Long, request: MovementRequest): Result<MovementDto> =
        safeCall { api.updateMovement(id, request) }

    suspend fun deleteMovement(id: Long): Result<Unit> = safeCall {
        val response = api.deleteMovement(id)
        if (!response.isSuccessful) throw HttpException(response)
    }

    // Convierte los errores técnicos en mensajes entendibles
    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        val message = when (e.code()) {
            400 -> "Datos inválidos. Revisa el formulario."
            401 -> "Tu sesión no es válida. Inicia sesión de nuevo."
            404 -> "El movimiento o la categoría no existe."
            409 -> "No se pudo completar por un conflicto de datos."
            else -> "Error del servidor. Intenta más tarde."
        }
        Result.failure(ApiException(e.code(), message))
    } catch (e: IOException) {
        Result.failure(ApiException(null, "No hay conexión con el servidor."))
    } catch (e: Exception) {
        Result.failure(ApiException(null, "Ocurrió un error inesperado."))
    }
}