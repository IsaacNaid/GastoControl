
package mx.edu.utez.gastocontrol.data.repository

import kotlinx.coroutines.flow.Flow
import mx.edu.utez.gastocontrol.data.local.CategoryDao
import mx.edu.utez.gastocontrol.data.model.CategoryEntity
import mx.edu.utez.gastocontrol.data.remote.CategoryApiService
import mx.edu.utez.gastocontrol.data.remote.CategoryRequestDto

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val api: CategoryApiService? = null
) {

    val allCategories: Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    suspend fun getCategoryById(id: Long): CategoryEntity? =
        categoryDao.getCategoryById(id)

    suspend fun insertCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category.copy(isSynced = false))

    suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category.copy(isSynced = false))

    suspend fun deleteCategory(category: CategoryEntity) =
        categoryDao.deleteCategory(category)

    suspend fun syncCategories(): Result<Int> = runCatching {
        val service = requireNotNull(api) {
            "API de categorías no configurada"
        }

        val localCategories = categoryDao.getAllCategoriesOnce()
        var synced = 0

        for (category in localCategories.filter { !it.isSynced }) {
            val request = CategoryRequestDto(
                name = category.name,
                type = category.type,
                color = category.color,
                icon = category.icon
            )

            val remote = if (category.remoteId == null) {
                service.createCategory(request)
            } else {
                service.updateCategory(category.remoteId, request)
            }

            categoryDao.updateCategory(
                category.copy(
                    remoteId = remote.id,
                    isSynced = true
                )
            )
            synced++
        }

        synced
    }
}
