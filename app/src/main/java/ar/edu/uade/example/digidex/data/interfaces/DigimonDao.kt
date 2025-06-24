package ar.edu.uade.example.digidex.data.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ar.edu.uade.example.digidex.data.entity.DigimonEntity

@Dao
interface DigimonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(digimons: List<DigimonEntity>)

    @Query("SELECT * FROM digimon_table")
    suspend fun getAll(): List<DigimonEntity>

    @Query("SELECT * FROM digimon_table WHERE isFavorite = 1 AND userId = :userId")
    suspend fun getFavorites(userId: String): List<DigimonEntity>

    @Query("UPDATE digimon_table SET isFavorite = :isFav WHERE name = :name")
    suspend fun setFavorite(name: String, isFav: Boolean)

    @Query("DELETE FROM digimon_table WHERE userId = :userId AND isFavorite = 1")
    suspend fun clearFavoritesForUser(userId: String)

}
