package ar.edu.uade.example.digidex.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ar.edu.uade.example.digidex.data.db.DigimonDatabase

class DigimonViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = DigimonDatabase.getInstance(application)
        val dao = db.digimonDao()
        return DigimonViewModel(dao) as T
    }
}
