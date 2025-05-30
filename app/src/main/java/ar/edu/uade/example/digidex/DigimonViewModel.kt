package ar.edu.uade.example.digidex

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import ar.edu.uade.example.digidex.util.damerauLevenshtein
import ar.edu.uade.example.digidex.util.normalizeName

class DigimonViewModel : ViewModel() {
    var digimonList by mutableStateOf<List<Digimon>>(emptyList())
    var dapiDigimonList by mutableStateOf<List<DapiDigimonSummary>>(emptyList())
    var isLoading by mutableStateOf(true)

private val digimonNameOverrides = mapOf(
    "Yokomon" to "Pyocomon",
    "Motimon" to "Mochimon",
    "Frigimon" to "Yukidarumon",
    "Piximon" to "Piccolomon",
    "Datamon" to "Nanomon",
    "DemiDevimon" to "Pico Devimon",
    "Myotismon" to "Vamdemon",
    "Pabumon" to "Bubbmon",
    "ShogunGekomon" to "Tonosama Gekomon",
    "Gatomon" to "Tailmon",
    "Mammothmon" to "Mammon",
    "SkullMeramon" to "Death Meramon",
    "Pumpkinmon" to "Pumpmon",
    "Lillymon" to "Lilimon",
    "Phantomon" to "Fantomon",
    "MegaKabuterimon" to "Atlur Kabuterimon (Blue)",
    "MagnaAngemon" to "Holy Angemon",
    "VenomMyotismon" to "Venom Vamdemon",
    "Salamon" to "Plotmon",
    "Chuumon" to "Tyumon",
    "Machinedramon" to "Mugendramon",
    "Piedmon" to "Piemon",
    "Puppetmon" to "Pinochimon",
    "Scorpiomon" to "Anomalocarimon",
    "Divermon" to "Hangyomon",
    "Mushroomon" to "Mushmon",
    "Deramon" to "Delumon",
    "Cherrymon" to "Jyureimon",
    "Garbagemon" to "Garbemon",
    "Vilemon" to "Evilmon",
    "Candlemon" to "Candmon",
    "Revolvermon" to "Revolmon",
    "Magnadramon" to "Holydramon",
    "Gorillamon" to "Gorimon",
    "Veedramon" to "V-dramon",
    "Phoenixmon" to "Hououmon",
    "Penguienmon" to "Penmon",
    "SnowAgumon" to "YukiAgumon",
    "Meteormon" to "Insekimon",
    "Piddomon" to "Pidmon",
    "Chibomon" to "Chicomon",
    "DemiVeemon" to "Chibimon",
    "Raidramon" to "Lighdramon",
    "ExVeemon" to "XV-mon",
    "Imperialdramon" to "Imperialdramon(Dragon Mode)",
    "Azulongmon" to "Qinglongmon",
    "Omnimon" to "Omegamon"

)

init {
    viewModelScope.launch {
        try {
            dapiDigimonList = loadAllDapiDigimons()
            Log.d("DigimonVM", "DAPI cargados: ${dapiDigimonList.size}")
        } catch (e: Exception) {
            Log.e("DigimonVM", "Error cargando Digimons de DAPI", e)
        }

        try {
            digimonList = DigimonApi.retrofitService.getAllDigimons()
        } catch (e: Exception) {
            Log.e("DigimonVM", "Error cargando Digimons", e)
        } finally {
            isLoading = false
        }
    }
}

    suspend fun getDigimonDetails(digimonName: String): DapiDigimonResponse? {
        // 1. Override manual
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
        var bestDistance = Int.MAX_VALUE

        while (true) {
            try {
                val response = DapiApi.retrofitService.getDigimonPage(page)

                for (digimon in response.content) {
                    val norm = normalizeName(digimon.name)

                    // Coincidencia exacta → salir
                    if (norm == normalized) {
                        Log.d("DigimonVM", "Match exacto en página $page: ${digimon.name}")
                        return DapiApi.retrofitService.getDigimonById(digimon.id.toString())
                    }

                    // Si no es exacto, ver si es el más parecido hasta ahora
                    val dist = damerauLevenshtein(normalized, norm)
                    if (dist < bestDistance) {
                        bestDistance = dist
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

        // Si hubo un match aproximado suficientemente bueno
        if (bestMatch != null) {
            val maxLen = maxOf(normalized.length, normalizeName(bestMatch.name).length)
            val similarity = 1.0 - (bestDistance.toDouble() / maxLen)
            Log.d("DigimonVM", "Mejor match aproximado: ${bestMatch.name} (score: $similarity)")

            if (similarity >= 0.74) {
                return try {
                    DapiApi.retrofitService.getDigimonById(bestMatch.id.toString())
                } catch (e: Exception) {
                    Log.e("DigimonVM", "Error cargando detalles de ${bestMatch.name}", e)
                    null
                }
            }
        }

        Log.d("DigimonVM", "No se encontró $digimonName en ninguna página")
        return null
    }





suspend fun loadAllDapiDigimons(): List<DapiDigimonSummary> {
    val allDigimons = mutableListOf<DapiDigimonSummary>()
    var page = 0
    var hasNext = true
    val maxPages = 10  // TEMPORAL

    while (hasNext && page < maxPages) {
        try {
            val response = DapiApi.retrofitService.getDigimonPage(page)
            allDigimons.addAll(response.content)
            hasNext = !response.pageable.nextPage.isNullOrEmpty()
            page++
        } catch (e: Exception) {
            Log.e("DigimonVM", "Error cargando página $page de DAPI", e)
            break
        }
    }

    return allDigimons
}


}
