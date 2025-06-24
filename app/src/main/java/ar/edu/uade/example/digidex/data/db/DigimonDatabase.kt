package ar.edu.uade.example.digidex.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ar.edu.uade.example.digidex.data.entity.DigimonEntity
import ar.edu.uade.example.digidex.data.interfaces.DigimonDao

@Database(entities = [DigimonEntity::class], version = 2)
abstract class DigimonDatabase : RoomDatabase() {
    abstract fun digimonDao(): DigimonDao

    companion object {
        @Volatile
        private var INSTANCE: DigimonDatabase? = null

        fun getInstance(context: Context): DigimonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DigimonDatabase::class.java,
                    "digimon_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
