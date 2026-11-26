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
import kotlinx.coroutines.launch
import mx.edu.utez.gastocontrol.data.model.MovementDto
import mx.edu.utez.gastocontrol.data.repository.MovementRepository
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale
import kotlin.collections.filter
import kotlin.collections.sortedWith

/** Valores exactos del contrato API */
const val TIPO_INGRESO = "INCOME"
const val TIPO_GASTO = "EXPENSE"

/** Usamos BigDecimal: nunca Float/Double para dinero. */
data class Totales(
    val ingresos: BigDecimal = BigDecimal.ZERO,
    val gastos: BigDecimal = BigDecimal.ZERO,
    val saldo: BigDecimal = BigDecimal.ZERO
)
data class GastoCategoria(
    val nombre: String,
    val total: BigDecimal,
    val proporcion: Float
)
fun calcularTotales(movimientos: List<MovementDto>): Totales {
    var ingresos = BigDecimal.ZERO
    var gastos = BigDecimal.ZERO

    for (movimiento in movimientos) {
        val monto = BigDecimal(movimiento.amount)
        when (movimiento.type) {
            TIPO_INGRESO -> ingresos = ingresos.add(monto)
            TIPO_GASTO -> gastos = gastos.add(monto)
        }
    }
    return Totales(ingresos = ingresos, gastos = gastos, saldo = ingresos.subtract(gastos))
}

/**
 * Agrupa SOLO los gastos por categoría, suma cada grupo y calcula
 * qué parte del gasto total representa. Ordena de mayor a menor.
 */
fun calcularGastosPorCategoria(movimientos: List<MovementDto>): List<GastoCategoria> {
    val gastos = movimientos.filter { it.type == TIPO_GASTO }
    val totalGastos = gastos.fold(BigDecimal.ZERO) { suma, m -> suma.add(BigDecimal(m.amount)) }
    if (totalGastos.signum() == 0)
        return emptyList()

    return gastos
        .groupBy { it.categoryName }
        .map { (nombre, lista) ->
            val total = lista.fold(BigDecimal.ZERO) { suma, m -> suma.add(BigDecimal(m.amount)) }
            GastoCategoria(
                nombre = nombre,
                total = total,
                proporcion = total.divide(totalGastos, 4, RoundingMode.HALF_UP).toFloat()
            )
        }
        .sortedByDescending { it.total }
}

/** Los N movimientos más recientes */
fun movimientosRecientes(movimientos: List<MovementDto>, cantidad: Int = 5): List<MovementDto> =
    movimientos
        .sortedWith(compareByDescending<MovementDto> { it.date }.thenByDescending { it.id })
        .take(cantidad)

fun formatoDinero(valor: BigDecimal): String {
    val texto = valor.abs().setScale(2, RoundingMode.HALF_UP).toPlainString()
    return if (valor.signum() < 0) "-\$$texto" else "\$$texto"
}

internal fun mensajeDeError(e: Exception): String = when (e) {
    is IOException -> "No se pudo conectar con el servidor. Revisa tu conexión."
    is NumberFormatException -> "El servidor envió un monto con formato inválido."
    else -> "Ocurrió un error inesperado. Intenta de nuevo."
}
data class Periodo(val desde: String, val hasta: String, val etiqueta: String)

private val MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

fun periodoMesActual(): Periodo {
    val cal = Calendar.getInstance()
    val anio = cal.get(Calendar.YEAR)
    val mes = cal.get(Calendar.MONTH) + 1 // Calendar cuenta los meses desde 0
    val ultimoDia = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return Periodo(
        desde = String.format(Locale.US, "%04d-%02d-01", anio, mes),
        hasta = String.format(Locale.US, "%04d-%02d-%02d", anio, mes, ultimoDia),
        etiqueta = "${MESES[mes - 1]} $anio"
    )
}
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Empty(val periodo: Periodo) : DashboardUiState
    data class Success(
        val periodo: Periodo,
        val totales: Totales,
        val gastosPorCategoria: List<GastoCategoria>,
        val recientes: List<MovementDto>
    ) : DashboardUiState
    data class Error(val mensaje: String) : DashboardUiState
}
/**
 * Pide los movimientos DEL MES ACTUAL al MovementRepository
 * (GET /api/movements?from=&to=) y prepara para el inicio
 * totales, gastos por categoría y movimientos recientes.
 */
class DashboardViewModel(
    private val repository: MovementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var cargaJob: Job? = null

    fun cargarResumen() {
        cargaJob?.cancel()
        _uiState.value = DashboardUiState.Loading
        val periodo = periodoMesActual()

        cargaJob = viewModelScope.launch {
            _uiState.value = try {
                val movimientos = repository.getMovements(from = periodo.desde, to = periodo.hasta)
                if (movimientos.isEmpty()) {
                    DashboardUiState.Empty(periodo)
                } else {
                    DashboardUiState.Success(
                        periodo = periodo,
                        totales = calcularTotales(movimientos),
                        gastosPorCategoria = calcularGastosPorCategoria(movimientos),
                        recientes = movimientosRecientes(movimientos)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DashboardUiState.Error(mensajeDeError(e))
            }
        }
    }
    companion object {
        fun factory(repository: MovementRepository) = viewModelFactory {
            initializer { DashboardViewModel(repository) }
        }
    }
}

