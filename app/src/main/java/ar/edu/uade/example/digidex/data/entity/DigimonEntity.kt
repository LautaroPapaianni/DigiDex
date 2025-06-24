package ar.edu.uade.example.digidex.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "digimon_table")
data class DigimonEntity(
    @PrimaryKey val name: String,
    val img: String,
    val level: String,
    val isFavorite: Boolean = false,
    val userId: String
)
