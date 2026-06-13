package ar.edu.uade.example.digidex.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.example.digidex.data.entity.DigimonEntity
import ar.edu.uade.example.digidex.data.interfaces.DigimonDao
import kotlinx.coroutines.launch
import android.util.Log
import ar.edu.uade.example.digidex.data.model.DapiDigimonResponse
import kotlin.text.equals

// Modelo de UI para la pantalla de detalle (puede ser tu DigimonEntity directamente o uno mapeado)
// Por simplicidad, podemos intentar usar DigimonEntity y añadirle el 'isFavorite' dinámicamente.
// O crear un modelo específico si prefieres mantener DigimonEntity solo para Room.

class DigimonDetailViewModel(
    private val digimonName: String,
    private val dao: DigimonDao,
    private val mainVmFavoriteNames: List<String>,
) : ViewModel() {
    private val viewModel: DigimonViewModel = DigimonViewModel(dao = dao)

    var uiState by mutableStateOf(DigimonDetailScreenState())
        private set

    init {
        loadDetails()
    }

    private fun loadDetails() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            try {
                // digimonName es el nombre de la lista (ej. "Agumon")
                var entityInRoom = dao.getByName(digimonName)

                if (entityInRoom == null || !entityInRoom.detailsFetchedFromDapi) {
                    Log.d("DetailVM", "No hay detalles en Room o no están completos para '$digimonName'. Llamando a DAPI...")
                    // getDigimonDetails usa 'digimonName' (de la lista) para su lógica de búsqueda,
                    // incluyendo el OverrideNameMap y la paginación.
                    val dapiResponse = viewModel.getDigimonDetails(digimonName) // Tu función crucial

                    if (dapiResponse != null) {
                        Log.d("DetailVM", "Detalles obtenidos de DAPI para '$digimonName'. DAPI name: '${dapiResponse.name}'.")
                        // IMPORTANTE: Creamos/actualizamos la entidad usando 'digimonName' (el de la lista) como clave
                        // pero con los datos de 'dapiResponse'.
                        entityInRoom = createOrUpdateEntityForRoom(entityInRoom, dapiResponse, digimonName)
                        dao.upsert(entityInRoom) // Guardar/Actualizar en Room
                        Log.d("DetailVM", "Entidad '$digimonName' actualizada en Room con detalles de DAPI.")
                    } else {
                        Log.w("DetailVM", "No se pudieron obtener detalles de DAPI para '$digimonName'.")
                        if (entityInRoom == null) {
                            uiState = uiState.copy(isLoading = false, error = "No se encontraron detalles para $digimonName", digimonData = null)
                            return@launch
                        }
                        // Si DAPI falló pero teníamos una entidad parcial, la mostramos.
                    }
                } else {
                    Log.d("DetailVM", "Detalles completos ya estaban en Room para '$digimonName'.")
                }

                uiState = uiState.copy(
                    digimonData = entityInRoom, // Esta entidad tiene name = "Agumon", pero descripciones de DAPI
                    isFavorite = mainVmFavoriteNames.contains(entityInRoom.name),
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                Log.e("DetailVM", "Excepción al cargar detalles para $digimonName", e)
                uiState = uiState.copy(isLoading = false, error = "Error: ${e.message}", digimonData = null)
            }
        }
    }

    // Nueva función para claridad o intégrala en la anterior
    private fun createOrUpdateEntityForRoom(
        existingEntity: DigimonEntity?, // La entidad de Room con name = "Agumon"
        dapiResponse: DapiDigimonResponse, // Respuesta de DAPI, podría tener name = "Agumon (Rookie)"
        nameForRoomKey: String // El nombre que DEBE usarse como clave en Room (ej. "Agumon")
    ): DigimonEntity {
        // Si no hay entidad existente, creamos una nueva con el 'nameForRoomKey'.
        // Si sí hay, la usamos como base y solo actualizamos los campos de DAPI.
        val baseImg = existingEntity?.img
        val baseLevel = existingEntity?.level

        return DigimonEntity(
            name = nameForRoomKey, // CLAVE: Usar el nombre de la lista para la entidad de Room
            img = dapiResponse.images.firstOrNull()?.href ?: baseImg, // Prioriza DAPI, luego base
            level = dapiResponse.levels.firstOrNull()?.level ?: baseLevel, // Prioriza DAPI, luego base

            // Campos de DAPI (estos vienen de dapiResponse)
            dapiId = dapiResponse.id,
            dapiImages = dapiResponse.images.map { it.href },
            dapiLevels = dapiResponse.levels.map { it.level },
            dapiAttributes = dapiResponse.attributes.map { it.attribute },
            dapiTypes = dapiResponse.types.map { it.type },
            dapiFields = dapiResponse.fields.map { it.field },
            dapiReleaseDate = dapiResponse.releaseDate,
            // IMPORTANTE: La descripción de DAPI
            dapiDescription = dapiResponse.descriptions.firstOrNull { it.language.equals("en_us", ignoreCase = true) }?.description,

            detailsFetchedFromDapi = true // Marcar como cargado
        )
    }
}