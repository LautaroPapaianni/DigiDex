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
import kotlinx.coroutines.launch

class DigimonViewModel(
    private val dao: DigimonDao
) : ViewModel() {

    val favoriteDigimonNames = mutableStateListOf<String>()

    private val db = Firebase.firestore
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    var digimonList by mutableStateOf<List<Digimon>>(emptyList())
    var isLoading by mutableStateOf(true)
    private var currentUserId: String? = null

    var searchText by mutableStateOf("")
    var sortOption by mutableStateOf("A-Z")
    var selectedLevel by mutableStateOf("Todos")

    init {
        viewModelScope.launch {
            Log.d("DigimonVM_Init", "ViewModel inicializándose...")
            loadDigimonsFromDbOrApi()
            Log.d("DigimonVM_Init", "Digimons base cargados en init. Total: ${digimonList.size}")

            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.uid?.let { userId ->
                Log.d("DigimonVM_Init", "Usuario ya logueado ($userId), cargando sus favoritos.")
                if (digimonList.isNotEmpty()) {
                    loadFavoritesFromFirestore(userId)
                } else {
                    Log.w("DigimonVM_Init", "digimonList vacía después de loadDigimons en init. No se cargan favoritos.")
                }
            } ?: run {
                Log.d("DigimonVM_Init", "No hay usuario logueado al inicio.")
            }
        }
    }

    fun prepareForLogout() {
        viewModelScope.launch {
            favoriteDigimonNames.clear()
            digimonList = emptyList()
            currentUserId = null
            FirebaseAuth.getInstance().currentUser?.uid
        }
    }

    fun prepareForViewModelResetAfterLogout() {
        favoriteDigimonNames.clear()
        digimonList = digimonList.map { it.copy(isFavorite = false) }
    }

    fun inicializarSesion() {
        clearLocalFavorites()
    }

    fun clearLocalFavorites() {
        uid?.let {
            viewModelScope.launch {
                favoriteDigimonNames.clear()
                digimonList = digimonList.map { it.copy(isFavorite = false) }
            }
        }
    }

    suspend fun loadDigimonsFromDbOrApi(){
        var entities = dao.getAll() // Intenta cargar desde Room primero
        Log.d("DigimonVM", "Digimons cargados desde Room: ${entities.size}")

        if (entities.isEmpty()) {
            Log.d("DigimonVM", "Room vacío, cargando desde API...")
            try {
                val apiDigimonList = DigimonApi.retrofitService.getAllDigimons() // Esto devuelve List<Digimon> (modelo API)
                val initialEntities = apiDigimonList.map { it.toInitialEntity() } // Convertir a List<DigimonEntity>
                dao.insertInitialBulk(initialEntities) // Guardar en Room
                entities = initialEntities // Usar estas entidades recién cargadas
                Log.d("DigimonVM", "Digimons cargados desde API y guardados en Room. Total: ${entities.size}")
            } catch (e: Exception) {
                Log.e("DigimonVM", "Fallo al cargar desde API: ${e.message}", e)
                // entities seguirá vacía si falla la API y Room estaba vacío
            }
        }
        digimonList = entities.toUiModelList(favoriteDigimonNames)
    }

    fun Digimon.toInitialEntity(): DigimonEntity { // Digimon es tu modelo de UI/API
        return DigimonEntity(
            name = this.name,
            img = this.img,
            level = this.level,
            detailsFetchedFromDapi = false // Inicialmente no se han cargado los detalles
            // No hay isFavorite ni userId aquí
        )
    }

    // Desde DigimonEntity (de Room) a tu modelo de UI Digimon
// Esta función ahora necesitará saber si es favorito basándose en tu lista 'favoriteDigimonNames'
    fun DigimonEntity.toUiModel(isFavorite: Boolean): Digimon {
        return Digimon(
            name = this.name,
            img = this.img ?: "", // Provee un default si img puede ser null
            level = this.level ?: "Desconocido", // Provee un default
            isFavorite = isFavorite // Se determina externamente
        )
    }

    fun List<DigimonEntity>.toUiModelList(favoriteDigimonNames: List<String>): List<Digimon> {
        return this.map { it.toUiModel(favoriteDigimonNames.contains(it.name)) }
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
            FirebaseAuth.getInstance().signOut() // Firebase signOut
            viewModelScope.launch {
                prepareForLogout()
                prepareForViewModelResetAfterLogout()
                onComplete() // Callback para la UI (navegar a pantalla de login, etc.)
            }
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
        val currentFirestoreInstance = Firebase.firestore
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("DigimonVM_Save", "Intentando guardar. UID actual de Auth: $currentUid. Digimon: ${digimon.name}")
        if (currentUid == null) {
            Log.e("DigimonVM_Save", "UID es null. No se puede guardar.")
            return
        }
        uid?.let {
            currentFirestoreInstance.collection("users")
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

    fun loadFavoritesFromFirestore(userId: String) {
        Log.d("DigimonVM", "Cargando favoritos para el usuario: $userId desde Firestore.")
            db.collection("users")
                .document(userId)
                .collection("favorites")
                .get()
                .addOnSuccessListener { favoriteDocs ->
                    viewModelScope.launch {
                        val entities = favoriteDocs.documents.mapNotNull { doc ->
                            val name = doc.getString("name")
                            val img = doc.getString("img")
                            val level = doc.getString("level")
                            if (name != null && img != null && level != null) {
                                DigimonEntity(name, img, level)
                            } else null
                        }
                        favoriteDigimonNames.clear()
                        favoriteDigimonNames.addAll(entities.map { it.name })
                        val currentDigimonListState = digimonList
                        Log.d("DigimonVM_Debug", "Nombres en favoriteDigimonNames ANTES del map: ${favoriteDigimonNames.joinToString()}")
                        Log.d("DigimonVM_Debug", "Primeros 5 nombres en currentDigimonListState (si existen): ${currentDigimonListState.take(5).map { it.name }.joinToString()}")
                        Log.d("DigimonVM_Debug", "Tamaño de favoriteDigimonNames: ${favoriteDigimonNames.size}")
                        Log.d("DigimonVM_Debug", "Tamaño de currentDigimonListState: ${currentDigimonListState.size}")
                        if (currentDigimonListState.isNotEmpty()) {
                            Log.d("DigimonVM_Map", "Aplicando favoritos. Lista de favoritos en memoria: ${favoriteDigimonNames.joinToString()}")
                            digimonList = currentDigimonListState.map { digimon ->
                                val isCurrentlyFavorite = favoriteDigimonNames.contains(digimon.name)
                                digimon.copy(isFavorite = isCurrentlyFavorite)
                            }
                            // Tu log existente
                            Log.d("DigimonVM", "digimonList actualizada con nuevos favoritos. Ejemplo: ${digimonList.firstOrNull()?.isFavorite}")
                        } else {
                            Log.w("DigimonVM", "digimonList está vacía al intentar aplicar favoritos. ¿Se cargaron los Digimon base?")
                            // Si digimonList está vacía, necesitas cargarla primero (desde API/Room general)
                            // y LUEGO aplicar los favoritos. Esto indica un problema de orden de operaciones.
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("DigimonVM", "Error al cargar favoritos desde Firestore para $userId", exception)
                    // Considera limpiar los favoritos en memoria igualmente para no mostrar datos obsoletos
                    viewModelScope.launch {
                        favoriteDigimonNames.clear()
                        val currentDigimonListState = digimonList
                        if (currentDigimonListState.isNotEmpty()) {
                            digimonList = currentDigimonListState.map { it.copy(isFavorite = false) }
                        }
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
}