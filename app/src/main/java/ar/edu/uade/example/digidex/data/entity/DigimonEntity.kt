package ar.edu.uade.example.digidex.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ar.edu.uade.example.digidex.data.db.converters.StringListConverter

@Entity(tableName = "digimon_info_table")
@TypeConverters(StringListConverter::class)
data class DigimonEntity(
    @PrimaryKey val name: String,
    var img: String?,
    var level: String?,
    var dapiId: Int? = null,
    var dapiImages: List<String>? = null,
    var dapiLevels: List<String>? = null,
    var dapiAttributes: List<String>? = null,
    var dapiTypes: List<String>? = null,
    var dapiFields: List<String>? = null,
    var dapiReleaseDate: String? = null,
    var dapiDescription: String? = null,
    var detailsFetchedFromDapi: Boolean = false
)