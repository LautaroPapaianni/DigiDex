package ar.edu.uade.example.digidex.viewmodel

import ar.edu.uade.example.digidex.data.entity.DigimonEntity

data class DigimonDetailScreenState(
    val digimonData: DigimonEntity? = null, // Datos de Room (con detalles de DAPI)
    val isFavorite: Boolean = false,       // Determinado por el DigimonViewModel principal
    val isLoading: Boolean = true,
    val error: String? = null
)