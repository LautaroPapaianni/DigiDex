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
import kotlin.text.any
import kotlin.text.equals

class DigimonViewModel(
    private val dao: DigimonDao
) : ViewModel() {

    // Los niveles base para que no empiece vacía
    private val OFFICIAL_LEVELS = listOf(
        "Fresh", "Training", "Rookie", "Champion", "Ultimate", "Mega", "Armor", "In Training"
    )

    // Ahora es una lista mutable que Compose puede observar
    val availableLevels = mutableStateListOf<String>()

    // Propiedad para que la Grid se actualice sola
    // Propiedad para que la Grid se actualice sola
    val filteredDigimons: List<Digimon>
        get() {
            val filtered = digimonList
                .filter { it.name.contains(searchText, ignoreCase = true) }
                .filter {
                    if (selectedLevel == "Todos") true
                    else it.level.equals(selectedLevel, ignoreCase = true)
                }
                .sortedWith(
                    compareByDescending<Digimon> { it.isFavorite }
                        .then(
                            when (sortOption) {
                                "A-Z" -> compareBy { it.name }
                                "Z-A" -> compareByDescending { it.name }
                                "Nivel ↑" -> compareBy { it.level }
                                "Nivel ↓" -> compareByDescending { it.level }
                                else -> compareBy { it.name }
                            }
                        )
                )

            // --- LOG PARA DEPURACIÓN ---
            Log.d("DigimonFilter", "Filtro: [$selectedLevel] | Busqueda: '$searchText' | Mostrando: ${filtered.size} Digimons")
            // ---------------------------
            // Log temporal para ver qué niveles existen realmente en memoria
            val conteoNiveles = digimonList.groupBy { it.level }.mapValues { it.value.size }
            Log.d("DigimonCheck", "Distribución actual en memoria: $conteoNiveles")
            return filtered
        }

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

    suspend fun loadDigimonsFromDbOrApi() {
        var entities = dao.getAll()

        if (entities.isEmpty()) {
            try {
                val apiDigimonList = DigimonApi.retrofitService.getAllDigimons()

                // Al cargar por primera vez, guardamos las entidades originales
                val initialEntities = apiDigimonList.map { it.toInitialEntity() }
                dao.insertInitialBulk(initialEntities)
                entities = initialEntities

                // Llenamos el menú con los niveles que vinieron de la API original
                // Dentro del bloque entities.isEmpty()
                val levelsFromApi = apiDigimonList.mapNotNull {
                    mapTechnicalLevel(it.level, it.name) // Normalizamos usando el nombre
                }
                    .filter { it.isNotBlank() && it != "Desconocido" }
                    .distinct()
                    .sorted()
                availableLevels.clear()
                availableLevels.addAll(levelsFromApi)

            } catch (e: Exception) {
                Log.e("DigimonVM", "Fallo al cargar desde API: ${e.message}")
            }
        } else {
            // SI YA HAY DATOS: No leemos los niveles de las entidades (porque DAPI las cambió).
            // Usamos directamente nuestra lista OFFICIAL_LEVELS para el menú.
            availableLevels.clear()
            availableLevels.addAll(OFFICIAL_LEVELS.sorted())
        }

        // Al convertir a UI Model, mapTechnicalLevel se encargará de que
        // si en la DB dice "Child", en el objeto Digimon diga "Rookie"
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
    // En DigimonViewModel.kt
    fun DigimonEntity.toUiModel(isFavorite: Boolean): Digimon {
        // Pasamos this.level y también this.name
        val normalizedLevel = mapTechnicalLevel(this.level, this.name)
        return Digimon(
            name = this.name,
            img = this.img ?: "",
            level = normalizedLevel,
            isFavorite = isFavorite
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
        // 1. Calculamos el nuevo estado basándonos en la lista de nombres (nuestra fuente de verdad)
        val isCurrentlyFavorite = favoriteDigimonNames.contains(digimon.name)
        val newFavoriteStatus = !isCurrentlyFavorite

        // 2. Actualizamos la lista de nombres de favoritos inmediatamente (UI principal reacciona)
        if (newFavoriteStatus) {
            if (!favoriteDigimonNames.contains(digimon.name)) {
                favoriteDigimonNames.add(digimon.name)
            }
        } else {
            favoriteDigimonNames.remove(digimon.name)
        }

        // 3. Actualizamos el objeto en la lista principal (para que la Grid sepa que cambió)
        val index = digimonList.indexOfFirst { it.name == digimon.name }
        if (index != -1) {
            digimonList = digimonList.toMutableList().also {
                it[index] = it[index].copy(isFavorite = newFavoriteStatus)
            }
        }

        // 4. Sincronizamos con la nube (Firestore)
        viewModelScope.launch {
            if (newFavoriteStatus) {
                saveFavoriteToFirestore(digimon)
            } else {
                removeFavoriteFromFirestore(digimon)
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

    // Agregamos 'name' como parámetro
    private fun mapTechnicalLevel(level: String?, name: String): String {
        val trimmedLevel = level?.trim() ?: return "Desconocido"

        // EXCEPCIONES ESPECÍFICAS (Megas que DAPI llama Ultimate)
        val knownMegas = listOf("Daemon", "HerculesKabuterimon", "Milleniummon")
        if (name in knownMegas && trimmedLevel.equals("Ultimate", ignoreCase = true)) {
            return "Mega"
        }

        return when {
            // --- FRESH ---
            trimmedLevel.contains("Fresh", ignoreCase = true) ||
                    trimmedLevel.equals("Baby I", ignoreCase = true) ||
                    trimmedLevel.equals("Slime", ignoreCase = true) -> "Fresh"

            // --- IN TRAINING ---
            trimmedLevel.contains("In Training", ignoreCase = true) ||
                    trimmedLevel.contains("Training", ignoreCase = true) || // Cubre ambos
                    trimmedLevel.equals("Baby II", ignoreCase = true) ||
                    trimmedLevel.equals("Lesser", ignoreCase = true) -> "In Training"

            // --- ROOKIE ---
            trimmedLevel.equals("Rookie", ignoreCase = true) ||
                    trimmedLevel.equals("Child", ignoreCase = true) -> "Rookie"

            // --- CHAMPION ---
            trimmedLevel.equals("Champion", ignoreCase = true) ||
                    trimmedLevel.equals("Adult", ignoreCase = true) -> "Champion"

            // --- ULTIMATE ---
            trimmedLevel.equals("Perfect", ignoreCase = true) -> "Ultimate"
            // Si no es un Mega conocido y dice Ultimate, lo dejamos como Ultimate (Stage 5 occidental)
            trimmedLevel.equals("Ultimate", ignoreCase = true) -> "Ultimate"

            // --- MEGA ---
            trimmedLevel.equals("Mega", ignoreCase = true) ||
                    trimmedLevel.contains("Mega", ignoreCase = true) -> "Mega"

            // --- ARMOR ---
            trimmedLevel.contains("Armor", ignoreCase = true) -> "Armor"

            else -> trimmedLevel
        }
    }

    // Función auxiliar para ayudar al mapeador con los casos ambiguos
    private fun isActuallyUltimate(level: String): Boolean {
        // Esta es una lista de seguridad para los niveles oficiales de DigimonAPI
        val westernLevels = listOf("Fresh", "In-Training", "Rookie", "Champion", "Ultimate", "Mega")
        return westernLevels.contains(level)
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