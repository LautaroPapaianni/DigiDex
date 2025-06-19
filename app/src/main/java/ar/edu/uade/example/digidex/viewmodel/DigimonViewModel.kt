package ar.edu.uade.example.digidex.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import ar.edu.uade.example.digidex.R
import ar.edu.uade.example.digidex.data.model.Digimon
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DigimonViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    var digimonList by mutableStateOf<List<Digimon>>(emptyList())
    var isLoading by mutableStateOf(true)

    var searchText by mutableStateOf("")
    var sortOption by mutableStateOf("A-Z")
    var selectedLevel by mutableStateOf("Todos")



    init {
        viewModelScope.launch {
            try {
                digimonList = DigimonApi.retrofitService.getAllDigimons()
                loadFavoritesFromFirestore()
            } catch (e: Exception) {
                Log.e("DigimonVM", "Error cargando Digimons", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun logout(context: Context, onComplete: () -> Unit) {
        val googleSignInClient = GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        googleSignInClient.signOut().addOnCompleteListener {
            FirebaseAuth.getInstance().signOut()
            onComplete()
        }
    }
    fun toggleFavorite(digimon: Digimon) {
        digimon.isFavorite = !digimon.isFavorite
        val index = digimonList.indexOfFirst { it.name == digimon.name }
        if (index != -1) {
            digimonList = digimonList.toMutableList().also { it[index] = digimon }
        }

        if (digimon.isFavorite) {
            saveFavoriteToFirestore(digimon)
        } else {
            removeFavoriteFromFirestore(digimon)
        }
    }

    fun saveFavoriteToFirestore(digimon: Digimon) {
        uid?.let {
            db.collection("users")
                .document(it)
                .collection("favorites")
                .document(digimon.name)
                .set(mapOf("name" to digimon.name, "img" to digimon.img, "level" to digimon.level))
        }
    }

    fun removeFavoriteFromFirestore(digimon: Digimon) {
        uid?.let {
            db.collection("users")
                .document(it)
                .collection("favorites")
                .document(digimon.name)
                .delete()
        }
    }

    fun loadFavoritesFromFirestore() {
        uid?.let {
            db.collection("users")
                .document(it)
                .collection("favorites")
                .get()
                .addOnSuccessListener { snapshot ->
                    val favoritesNames = snapshot.documents.mapNotNull { it.id }
                    digimonList = digimonList.map {
                        it.copy(isFavorite = favoritesNames.contains(it.name))
                    }
                }
        }
    }
}