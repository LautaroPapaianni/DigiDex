package ar.edu.uade.example.digidex.viewmodel

import DigimonApi
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.example.digidex.R
import ar.edu.uade.example.digidex.data.entity.DigimonEntity
import ar.edu.uade.example.digidex.data.interfaces.DigimonDao
import ar.edu.uade.example.digidex.data.model.Digimon
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class DigimonViewModel(
    private val dao: DigimonDao
) : ViewModel() {

    val favoriteDigimonNames = mutableStateListOf<String>()

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
                loadDigimons()
                uid?.let {
                    loadFavoritesFromFirestore()
                }
            } catch (e: Exception) {
                Log.e("DigimonVM", "Error en init", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun syncFavoritesFromFirestoreToRoom(userId: String) {
        val userDoc = FirebaseFirestore.getInstance().collection("users").document(userId)
        userDoc.get().addOnSuccessListener { doc ->
            val favorites = doc.get("favorites") as? List<String> ?: emptyList()
            viewModelScope.launch {
                dao.clearFavoritesForUser(userId)
                favorites.forEach { name ->
                    dao.getFavorites(name)
                }
                loadFavoritesFromDb(userId)
            }
        }.addOnFailureListener {
            Log.e("DigimonVM", "Error al sincronizar favoritos desde Firestore", it)
        }
    }

    suspend fun inicializarSesion(userId: String) {
        clearLocalFavorites()
        loadFavoritesFromFirestore()
        syncFavoritesFromFirestoreToRoom(userId)
        loadDigimons()
    }

    fun clearLocalFavorites() {
        uid?.let {
            viewModelScope.launch {
                dao.clearFavoritesForUser(it)
                favoriteDigimonNames.clear()
                digimonList = digimonList.map { it.copy(isFavorite = false) }
            }
        }
    }

    fun loadFavoritesFromDb(userId: String) {
        viewModelScope.launch {
            val favoritesFromDb = dao.getFavorites(userId).map { it.name }
            favoriteDigimonNames.clear()
            favoriteDigimonNames.addAll(favoritesFromDb)
        }
    }

    suspend fun loadDigimons() {
        try {
            val remoteList = DigimonApi.retrofitService.getAllDigimons()
            digimonList = remoteList
            dao.insertAll(remoteList.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("DigimonVM", "Fallo conexión, usando Room: ${e.message}", e)
            digimonList = dao.getAll().map { it.toModel() }
        }
    }

    fun Digimon.toEntity(): DigimonEntity = DigimonEntity(name, img, level, isFavorite,
        uid.toString()
    )
    fun DigimonEntity.toModel(): Digimon = Digimon(name, img, level, isFavorite)

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

        viewModelScope.launch {
            val isFav = digimon.name in favoriteDigimonNames
            dao.setFavorite(digimon.name, !isFav)
            if (isFav) {
                favoriteDigimonNames.remove(digimon.name)
                removeFavoriteFromFirestore(digimon)
            } else {
                favoriteDigimonNames.add(digimon.name)
                saveFavoriteToFirestore(digimon)
            }
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
        uid?.let { currentUid ->
            db.collection("users")
                .document(currentUid)
                .collection("favorites")
                .get()
                .addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        dao.clearFavoritesForUser(currentUid)

                        val entities = snapshot.documents.mapNotNull { doc ->
                            val name = doc.getString("name")
                            val img = doc.getString("img")
                            val level = doc.getString("level")
                            if (name != null && img != null && level != null) {
                                DigimonEntity(name, img, level, true, currentUid)
                            } else null
                        }
                        dao.insertAll(entities)

                        favoriteDigimonNames.clear()
                        favoriteDigimonNames.addAll(entities.map { it.name })

                        digimonList = digimonList.map {
                            it.copy(isFavorite = favoriteDigimonNames.contains(it.name))
                        }
                    }
                }
        }
    }

}