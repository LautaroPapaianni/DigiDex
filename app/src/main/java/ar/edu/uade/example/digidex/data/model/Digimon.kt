package ar.edu.uade.example.digidex.data.model

data class Digimon (
    val name: String,
    val img: String,
    val level: String,
    var isFavorite: Boolean = false,
)