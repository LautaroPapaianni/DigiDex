package ar.edu.uade.example.digidex.data.interfaces

import androidx.room.*
import ar.edu.uade.example.digidex.data.entity.DigimonEntity

@Dao
interface DigimonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialBulk(digimons: List<DigimonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(digimon: DigimonEntity)

    @Query("SELECT * FROM digimon_info_table ORDER BY name ASC")
    suspend fun getAll(): List<DigimonEntity>

    @Query("SELECT * FROM digimon_info_table WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): DigimonEntity?

    @Query("SELECT COUNT(*) FROM digimon_info_table")
    suspend fun count(): Int

    @Query("DELETE FROM digimon_info_table")
    suspend fun clearAll()
}