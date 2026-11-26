
package mx.edu.utez.gastocontrol.ui.category

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.edu.utez.gastocontrol.data.local.GastoControlDatabase
import mx.edu.utez.gastocontrol.data.model.CategoryEntity
import mx.edu.utez.gastocontrol.data.remote.CategoryRetrofitClient
import mx.edu.utez.gastocontrol.data.repository.CategoryRepository

class CategoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    // BASE DE DATOS LOCAL ROOM
    private val database =
        GastoControlDatabase.getDatabase(application)

    // REPOSITORIO CON ROOM Y RETROFIT
    private val repository = CategoryRepository(
        database.categoryDao(),
        CategoryRetrofitClient.api
    )

    // LISTA DE CATEGORIAS
    private val _categories =
        MutableStateFlow<List<CategoryEntity>>(emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        _categories.asStateFlow()

    // MENSAJES DE ERROR
    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()

    // ESTADO DE SINCRONIZACION
    private val _isSyncing =
        MutableStateFlow(false)

    val isSyncing: StateFlow<Boolean> =
        _isSyncing.asStateFlow()

    init {
        // OBSERVAR LOS CAMBIOS DE ROOM
        viewModelScope.launch {
            repository.allCategories.collect { lista ->
                _categories.value = lista
            }
        }
    }

    // CREAR CATEGORIA
    fun addCategory(
        name: String,
        type: String,
        color: String = "#2563EB",
        icon: String = "category"
    ) {
        if (name.isBlank()) {
            _error.value = "El nombre es obligatorio"
            return
        }

        if (type != "INCOME" && type != "EXPENSE") {
            _error.value = "Tipo de categoría inválido"
            return
        }

        viewModelScope.launch {
            try {
                repository.insertCategory(
                    CategoryEntity(
                        name = name.trim(),
                        type = type,
                        color = color,
                        icon = icon
                    )
                )
                _error.value = null
            } catch (e: Exception) {
                _error.value =
                    "No se pudo guardar la categoría"
            }
        }
    }

    // ACTUALIZAR CATEGORIA
    fun updateCategory(category: CategoryEntity) {
        if (category.name.isBlank()) {
            _error.value = "El nombre es obligatorio"
            return
        }

        if (
            category.type != "INCOME" &&
            category.type != "EXPENSE"
        ) {
            _error.value = "Tipo de categoría inválido"
            return
        }

        viewModelScope.launch {
            try {
                repository.updateCategory(
                    category.copy(
                        name = category.name.trim(),
                        isSynced = false
                    )
                )
                _error.value = null
            } catch (e: Exception) {
                _error.value =
                    "No se pudo actualizar la categoría"
            }
        }
    }

    // ELIMINAR CATEGORIA
    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(category)
                _error.value = null
            } catch (e: Exception) {
                _error.value =
                    "No se pudo eliminar la categoría"
            }
        }
    }

    // SINCRONIZAR CATEGORIAS CON SPRING BOOT
    fun syncCategories() {
        if (_isSyncing.value) return

        viewModelScope.launch {
            _isSyncing.value = true

            try {
                val result = repository.syncCategories()

                result.onSuccess {
                    _error.value = null
                }

                result.onFailure {
                    _error.value =
                        "No se pudo sincronizar con el servidor"
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // LIMPIAR MENSAJES DE ERROR
    fun clearError() {
        _error.value = null
    }
}
