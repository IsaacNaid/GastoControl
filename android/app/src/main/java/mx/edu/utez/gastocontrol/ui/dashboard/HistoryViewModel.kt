package mx.edu.utez.gastocontrol.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.gastocontrol.data.model.MovementDto
import mx.edu.utez.gastocontrol.data.repository.MovementRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Filtros del historial
data class FiltrosHistorial(
    val tipo: String? = null,
    val desde: String = "",
    val hasta: String = ""
)

// Estados de la pantalla de historial
sealed interface HistoryUiState {

    data object Loading : HistoryUiState

    data class Empty(
        val filtros: FiltrosHistorial
    ) : HistoryUiState

    data class Success(
        val movimientos: List<MovementDto>,
        val totales: Totales,
        val filtros: FiltrosHistorial
    ) : HistoryUiState

    data class Error(
        val mensaje: String,
        // true cuando el servidor respondió 401: la pantalla debe volver al login
        val sesionInvalida: Boolean = false
    ) : HistoryUiState
}

/*
 * Convierte "AAAA-MM-DD" en una fecha REAL.
 * ISO_LOCAL_DATE es estricto: rechaza 2026-99-99, 2026-02-30 o 2026-1-5.
 * Devuelve null si el texto no es una fecha válida.
 * (minSdk 33, así que java.time está disponible sin configuración extra)
 */
private fun parsearFecha(texto: String): LocalDate? =
    try {
        LocalDate.parse(texto, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: DateTimeParseException) {
        null
    }

// Validacion de fechas: formato, fecha existente y rango correcto
fun validarFechas(desde: String, hasta: String): String? {

    val fechaDesde = if (desde.isNotEmpty()) parsearFecha(desde) else null
    val fechaHasta = if (hasta.isNotEmpty()) parsearFecha(hasta) else null

    if (desde.isNotEmpty() && fechaDesde == null) {
        return "La fecha Desde no es válida. Usa AAAA-MM-DD (ej. 2026-11-01)."
    }

    if (hasta.isNotEmpty() && fechaHasta == null) {
        return "La fecha Hasta no es válida. Usa AAAA-MM-DD (ej. 2026-11-30)."
    }

    if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
        return "Desde no puede ser posterior a Hasta."
    }

    return null
}

// ViewModel del historial de movimientos
class HistoryViewModel(
    private val repository: MovementRepository
) : ViewModel() {

    private val _filtros = MutableStateFlow(FiltrosHistorial())

    val filtros: StateFlow<FiltrosHistorial> =
        _filtros.asStateFlow()

    private val _errorFiltro = MutableStateFlow<String?>(null)

    val errorFiltro: StateFlow<String?> =
        _errorFiltro.asStateFlow()

    private val _uiState = MutableStateFlow<HistoryUiState>(
        HistoryUiState.Loading
    )

    val uiState: StateFlow<HistoryUiState> =
        _uiState.asStateFlow()

    private var cargaJob: Job? = null

    fun cambiarTipo(tipo: String?) {
        _filtros.update {
            it.copy(tipo = tipo)
        }

        cargarHistorial()
    }

    fun cambiarDesde(texto: String) {
        _filtros.update {
            it.copy(desde = texto)
        }
    }

    fun cambiarHasta(texto: String) {
        _filtros.update {
            it.copy(hasta = texto)
        }
    }

    fun limpiarFiltros() {
        _filtros.value = FiltrosHistorial()

        cargarHistorial()
    }

    fun cargarHistorial() {

        val actuales = _filtros.value

        val filtrosLimpios = actuales.copy(
            desde = actuales.desde.trim(),
            hasta = actuales.hasta.trim()
        )

        val error = validarFechas(
            filtrosLimpios.desde,
            filtrosLimpios.hasta
        )

        _errorFiltro.value = error

        if (error != null) return

        cargaJob?.cancel()

        _uiState.value = HistoryUiState.Loading

        cargaJob = viewModelScope.launch {

            _uiState.value = try {

                val movimientos = repository.getMovements(
                    type = filtrosLimpios.tipo,
                    from = filtrosLimpios.desde.ifEmpty { null },
                    to = filtrosLimpios.hasta.ifEmpty { null }
                ).getOrThrow()

                if (movimientos.isEmpty()) {

                    HistoryUiState.Empty(filtrosLimpios)

                } else {

                    HistoryUiState.Success(
                        movimientos = movimientos,
                        totales = calcularTotales(movimientos),
                        filtros = filtrosLimpios
                    )
                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                HistoryUiState.Error(
                    mensaje = mensajeDeError(e),
                    sesionInvalida = esSesionInvalida(e)
                )
            }
        }
    }

    companion object {

        fun factory(repository: MovementRepository) =
            viewModelFactory {
                initializer {
                    HistoryViewModel(repository)
                }
            }
    }
}