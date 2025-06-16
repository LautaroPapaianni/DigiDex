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
import ar.edu.uade.example.digidex.data.model.DapiDigimonResponse
import ar.edu.uade.example.digidex.data.model.DapiDigimonSummary
import ar.edu.uade.example.digidex.data.model.Digimon
import ar.edu.uade.example.digidex.data.remote.DapiApi
import ar.edu.uade.example.digidex.utils.damerauLevenshtein
import ar.edu.uade.example.digidex.utils.normalizeName
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

    suspend fun getDigimonDetails(digimonName: String): DapiDigimonResponse? {
         try {
             val digimon = DapiApi.retrofitService.getDigimonByName(digimonName)
             Log.d("DigimonVM", "Digimon encontrado")
             return digimon

         } catch (_: Exception){
        }
        digimonNameOverrides[digimonName]?.let { overrideName ->
            Log.d("DigimonVM", "Usando override para $digimonName → $overrideName")
            return try {
                DapiApi.retrofitService.getDigimonByName(overrideName)
            } catch (e: Exception) {
                Log.e("DigimonVM", "Error al obtener override $overrideName", e)
                null
            }
        }

        val normalized = normalizeName(digimonName)
        var page = 0
        var bestMatch: DapiDigimonSummary? = null
        var bestSimilarity = 0.0

        while (true) {
            try {
                if (page % 5 == 0){
                    Log.d("DigimonVM", "Pagina: $page")
                }
                val response = DapiApi.retrofitService.getDigimonPage(page)

                for (digimon in response.content) {
                    val candidateName = digimon.name
                    val normalizedCandidate = normalizeName(candidateName)

                    if (normalized == normalizedCandidate) {
                        Log.d("DigimonVM", "Match exacto en página $page: $candidateName")
                        return DapiApi.retrofitService.getDigimonById(digimon.id.toString())
                    }

                    val distance = damerauLevenshtein(normalized, normalizedCandidate)
                    val maxLen = maxOf(normalized.length, normalizedCandidate.length)
                    val similarity = 1.0 - (distance.toDouble() / maxLen)

                    if (similarity >= 0.88) {
                        Log.d(
                            "DigimonVM",
                            "Match muy cercano (≥ 0.88) en página $page: $candidateName (score: $similarity)"
                        )
                        return DapiApi.retrofitService.getDigimonById(digimon.id.toString())
                    }

                    if (similarity > bestSimilarity) {
                        bestSimilarity = similarity
                        bestMatch = digimon
                    }
                }

                if (response.pageable.nextPage.isNullOrEmpty()) break
                page++

            } catch (e: Exception) {
                Log.e("DigimonVM", "Error buscando $digimonName en página $page", e)
                break
            }
        }

        if (bestMatch != null && bestSimilarity >= 0.74) {
            Log.d(
                "DigimonVM",
                "Match aceptable para $digimonName: ${bestMatch.name} (score: $bestSimilarity)"
            )
            return try {
                DapiApi.retrofitService.getDigimonById(bestMatch.id.toString())
            } catch (e: Exception) {
                Log.e("DigimonVM", "Error cargando detalles de ${bestMatch.name}", e)
                null
            }
        }

        Log.d("DigimonVM", "No se encontró $digimonName en ninguna página")
        return null
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