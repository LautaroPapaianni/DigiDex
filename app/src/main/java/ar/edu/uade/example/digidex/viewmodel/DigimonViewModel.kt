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
    private var currentUserId: String? = null

    var searchText by mutableStateOf("")
    var sortOption by mutableStateOf("A-Z")
    var selectedLevel by mutableStateOf("Todos")

    init {
        viewModelScope.launch {
            Log.d("DigimonVM_Init", "ViewModel inicializándose...")
            // Primero, cargar la lista base de Digimon
            loadDigimons() // Espera a que se complete
            Log.d("DigimonVM_Init", "Digimons base cargados en init. Total: ${digimonList.size}")

            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.uid?.let { userId ->
                Log.d("DigimonVM_Init", "Usuario ya logueado ($userId), cargando sus favoritos.")
                if (digimonList.isNotEmpty()) { // Comprobación adicional
                    loadFavoritesFromFirestore(userId)
                } else {
                    Log.w("DigimonVM_Init", "digimonList vacía después de loadDigimons en init. No se cargan favoritos.")
                }
            } ?: run {
                Log.d("DigimonVM_Init", "No hay usuario logueado al inicio.")
            }
            // isLoading = false // Asegúrate que isLoading se maneje correctamente en loadDigimons y loadFavorites
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
        // currentUserId = null
        // Si tienes otros estados específicos del usuario, resetealos aquí.
        // Considera recargar la lista de Digimon base si es necesario
        // viewModelScope.launch { loadDigimons() } // Para mostrar la lista "limpia"
    }

    fun inicializarSesion(userId: String) {
        clearLocalFavorites()
        syncFavoritesFromFirestoreToRoom(userId)
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
            kotlinx.coroutines.delay(1000)
            val remoteList = DigimonApi.retrofitService.getAllDigimons()
            digimonList = remoteList
            dao.insertAll(remoteList.map { it.toEntity() })
            Log.d("DigimonVM", "loadDigimons: Digimons cargados desde API. Total: ${digimonList.size}")
        } catch (e: Exception) {
            Log.e("DigimonVM", "Fallo conexión, usando Room: ${e.message}", e)
            digimonList = dao.getAll().map { it.toModel() } // Cargar desde Room como fallback
            Log.d("DigimonVM", "loadDigimons: Digimons cargados desde Room. Total: ${digimonList.size}")
        } finally {
            isLoading = false // Indicar fin de carga
        }
    }

    fun Digimon.toEntity(): DigimonEntity = DigimonEntity(name, img, level, isFavorite,
        uid.toString()
    )
    fun DigimonEntity.toModel(): Digimon = Digimon(name, img, level, isFavorite)

    fun logout(context: Context, onComplete: () -> Unit) {
        val userIdToClear = FirebaseAuth.getInstance().currentUser?.uid // Obtener UID ANTES de signOut

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
                userIdToClear?.let {
                    dao.clearFavoritesForUser(it) // Limpiar Room para el usuario deslogueado
                }
                // Llamar a la función que limpia el estado del ViewModel
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
                        dao.clearFavoritesForUser(userId)

                        val entities = favoriteDocs.documents.mapNotNull { doc ->
                            val name = doc.getString("name")
                            val img = doc.getString("img")
                            val level = doc.getString("level")
                            if (name != null && img != null && level != null) {
                                DigimonEntity(name, img, level, true, userId)
                            } else null
                        }
                        dao.insertAll(entities)

                        favoriteDigimonNames.clear()
                        favoriteDigimonNames.addAll(entities.map { it.name })
                        dao.clearFavoritesForUser(userId)
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

}