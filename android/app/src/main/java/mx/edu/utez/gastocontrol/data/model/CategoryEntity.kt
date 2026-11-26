
package mx.edu.utez.gastocontrol.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["remoteId"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,

    val remoteId: Long? = null,

    val name: String,

    val type: String,

    val color: String = "#2563EB",

    val icon: String = "category",

    val isSynced: Boolean = false
)
