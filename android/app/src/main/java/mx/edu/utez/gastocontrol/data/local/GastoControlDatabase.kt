
package mx.edu.utez.gastocontrol.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mx.edu.utez.gastocontrol.data.model.CategoryEntity

@Database(
    entities = [CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GastoControlDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao

    companion object {

        @Volatile
        private var INSTANCE: GastoControlDatabase? = null

        fun getDatabase(context: Context): GastoControlDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GastoControlDatabase::class.java,
                    "gastocontrol_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
