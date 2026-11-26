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
        val mensaje: String
    ) : HistoryUiState
}

private val FORMATO_FECHA = Regex("""\d{4}-\d{2}-\d{2}""")

// Validacion de fechas
fun validarFechas(desde: String, hasta: String): String? {

    if (desde.isNotEmpty() && !FORMATO_FECHA.matches(desde)) {
        return "La fecha Desde debe tener formato AAAA-MM-DD."
    }

    if (hasta.isNotEmpty() && !FORMATO_FECHA.matches(hasta)) {
        return "La fecha Hasta debe tener formato AAAA-MM-DD."
    }

    if (
        desde.isNotEmpty() &&
        hasta.isNotEmpty() &&
        desde > hasta
    ) {
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

                HistoryUiState.Error(mensajeDeError(e))
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