package ar.edu.uade.example.digidex.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ar.edu.uade.example.digidex.data.interfaces.DigimonDao

class DigimonDetailViewModelFactory(
    private val digimonName: String,
    private val dao: DigimonDao,
    private val mainVmFavoriteNames: List<String>,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DigimonDetailViewModel::class.java)) {
            return DigimonDetailViewModel(digimonName, dao, mainVmFavoriteNames) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for Detail screen")
    }
}