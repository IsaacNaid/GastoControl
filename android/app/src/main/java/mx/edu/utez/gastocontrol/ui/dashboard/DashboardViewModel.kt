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
import mx.edu.utez.gastocontrol.data.repository.ApiException
import mx.edu.utez.gastocontrol.data.repository.MovementRepository
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale

// Tipos de movimientos del contrato API
const val TIPO_INGRESO = "INCOME"
const val TIPO_GASTO = "EXPENSE"

// Totales de ingresos, gastos y saldo
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

    return Totales(
        ingresos = ingresos,
        gastos = gastos,
        saldo = ingresos.subtract(gastos)
    )
}

// Agrupa los gastos por categoria
fun calcularGastosPorCategoria(
    movimientos: List<MovementDto>
): List<GastoCategoria> {

    val gastos = movimientos.filter {
        it.type == TIPO_GASTO
    }

    val totalGastos = gastos.fold(BigDecimal.ZERO) { suma, movimiento ->
        suma.add(BigDecimal(movimiento.amount))
    }

    if (totalGastos.signum() == 0) {
        return emptyList()
    }

    return gastos
        .groupBy { it.categoryName }
        .map { (nombre, lista) ->

            val total = lista.fold(BigDecimal.ZERO) { suma, movimiento ->
                suma.add(BigDecimal(movimiento.amount))
            }

            GastoCategoria(
                nombre = nombre,
                total = total,
                proporcion = total.divide(
                    totalGastos,
                    4,
                    RoundingMode.HALF_UP
                ).toFloat()
            )
        }
        .sortedByDescending { it.total }
}

// Obtiene los movimientos mas recientes
fun movimientosRecientes(
    movimientos: List<MovementDto>,
    cantidad: Int = 5
): List<MovementDto> =
    movimientos
        .sortedWith(
            compareByDescending<MovementDto> { it.date }
                .thenByDescending { it.id }
        )
        .take(cantidad)

// Formato de cantidades monetarias
fun formatoDinero(valor: BigDecimal): String {
    val texto = valor.abs()
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString()

    return if (valor.signum() < 0) {
        "-\$$texto"
    } else {
        "\$$texto"
    }
}

// Manejo de errores compatible con MovementRepository
internal fun mensajeDeError(e: Exception): String = when (e) {

    is ApiException ->
        e.message ?: "Ocurrió un error en la solicitud."

    is IOException ->
        "No se pudo conectar con el servidor. Revisa tu conexión."

    is NumberFormatException ->
        "El servidor envió un monto con formato inválido."

    else ->
        "Ocurrió un error inesperado. Intenta de nuevo."
}

data class Periodo(
    val desde: String,
    val hasta: String,
    val etiqueta: String
)

private val MESES = listOf(
    "Enero",
    "Febrero",
    "Marzo",
    "Abril",
    "Mayo",
    "Junio",
    "Julio",
    "Agosto",
    "Septiembre",
    "Octubre",
    "Noviembre",
    "Diciembre"
)

// Obtiene el periodo del mes actual
fun periodoMesActual(): Periodo {
    val cal = Calendar.getInstance()

    val anio = cal.get(Calendar.YEAR)
    val mes = cal.get(Calendar.MONTH) + 1
    val ultimoDia = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    return Periodo(
        desde = String.format(
            Locale.US,
            "%04d-%02d-01",
            anio,
            mes
        ),
        hasta = String.format(
            Locale.US,
            "%04d-%02d-%02d",
            anio,
            mes,
            ultimoDia
        ),
        etiqueta = "${MESES[mes - 1]} $anio"
    )
}

// Estados de la pantalla principal
sealed interface DashboardUiState {

    data object Loading : DashboardUiState

    data class Empty(
        val periodo: Periodo
    ) : DashboardUiState

    data class Success(
        val periodo: Periodo,
        val totales: Totales,
        val gastosPorCategoria: List<GastoCategoria>,
        val recientes: List<MovementDto>
    ) : DashboardUiState

    data class Error(
        val mensaje: String
    ) : DashboardUiState
}

// ViewModel de la pantalla principal
class DashboardViewModel(
    private val repository: MovementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(
        DashboardUiState.Loading
    )

    val uiState: StateFlow<DashboardUiState> =
        _uiState.asStateFlow()

    private var cargaJob: Job? = null

    fun cargarResumen() {

        cargaJob?.cancel()

        _uiState.value = DashboardUiState.Loading

        val periodo = periodoMesActual()

        cargaJob = viewModelScope.launch {

            _uiState.value = try {

                val movimientos = repository.getMovements(
                    from = periodo.desde,
                    to = periodo.hasta
                ).getOrThrow()

                if (movimientos.isEmpty()) {

                    DashboardUiState.Empty(periodo)

                } else {

                    DashboardUiState.Success(
                        periodo = periodo,
                        totales = calcularTotales(movimientos),
                        gastosPorCategoria = calcularGastosPorCategoria(
                            movimientos
                        ),
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

        fun factory(repository: MovementRepository) =
            viewModelFactory {
                initializer {
                    DashboardViewModel(repository)
                }
            }
    }
}