package mx.edu.utez.gastocontrol.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.gastocontrol.data.model.MovementDto
import mx.edu.utez.gastocontrol.data.model.MovementRequest
import mx.edu.utez.gastocontrol.data.repository.ApiException
import mx.edu.utez.gastocontrol.data.repository.MovementRepository

sealed interface ListState {
    data object Loading : ListState
    data object Empty : ListState
    data class Success(val movements: List<MovementDto>) : ListState
    data class Error(val message: String) : ListState
}

sealed interface DetailState {
    data object Loading : DetailState
    data class Success(val movement: MovementDto) : DetailState
    data class Error(val message: String) : DetailState
}

// Estado de guardar y eliminar
data class ActionState(
    val working: Boolean = false,
    val done: Boolean = false,
    val error: String? = null
)

class MovementViewModel(private val repository: MovementRepository) : ViewModel() {

    private val _listState = MutableStateFlow<ListState>(ListState.Loading)
    val listState: StateFlow<ListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<DetailState>(DetailState.Loading)
    val detailState: StateFlow<DetailState> = _detailState.asStateFlow()

    private val _actionState = MutableStateFlow(ActionState())
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val _filter = MutableStateFlow<String?>(null)
    val filter: StateFlow<String?> = _filter.asStateFlow()

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    private var listJob: Job? = null
    private var detailJob: Job? = null

    fun loadMovements() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            _listState.value = ListState.Loading
            repository.getMovements(type = _filter.value)
                .onSuccess {
                    _listState.value =
                        if (it.isEmpty()) ListState.Empty else ListState.Success(it)
                }
                .onFailure { _listState.value = ListState.Error(handle(it)) }
        }
    }

    fun changeFilter(type: String?) {
        _filter.value = type
        loadMovements()
    }

    fun loadDetail(id: Long) {
        detailJob?.cancel()
        _detailState.value = DetailState.Loading
        detailJob = viewModelScope.launch {
            repository.getMovement(id)
                .onSuccess { _detailState.value = DetailState.Success(it) }
                .onFailure { _detailState.value = DetailState.Error(handle(it)) }
        }
    }

    // id == null crea (POST); con id edita (PUT)
    fun save(id: Long?, request: MovementRequest) = runAction {
        if (id == null) repository.createMovement(request)
        else repository.updateMovement(id, request)
    }

    fun delete(id: Long) = runAction { repository.deleteMovement(id) }

    private fun <T> runAction(block: suspend () -> Result<T>) {
        if (_actionState.value.working) return   // Evita doble toque
        _actionState.value = ActionState(working = true)
        viewModelScope.launch {
            block()
                .onSuccess {
                    _actionState.value = ActionState(done = true)
                    loadMovements()
                }
                .onFailure { _actionState.value = ActionState(error = handle(it)) }
        }
    }

    fun resetAction() {
        _actionState.update { if (it.working) it else ActionState() }
    }

    fun sessionHandled() {
        _sessionExpired.value = false
    }

    private fun handle(error: Throwable): String {
        if ((error as? ApiException)?.code == 401) _sessionExpired.value = true
        return error.message ?: "Ocurrió un error."
    }

    companion object {
        fun factory(repository: MovementRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { MovementViewModel(repository) } }
    }
}