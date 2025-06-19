package ar.edu.uade.example.digidex.ui.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import ar.edu.uade.example.digidex.viewmodel.DigimonViewModel
import ar.edu.uade.example.digidex.data.model.DapiDigimonResponse
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import ar.edu.uade.example.digidex.data.model.DapiDigimonSummary
import ar.edu.uade.example.digidex.data.remote.DapiApi
import ar.edu.uade.example.digidex.utils.damerauLevenshtein
import ar.edu.uade.example.digidex.utils.normalizeName
import ar.edu.uade.example.digidex.viewmodel.digimonNameOverrides

@Composable
fun DigimonDetailScreen(name: String, viewModel: DigimonViewModel, navController: NavController) {
    val decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8.toString())

    var digimon by remember { mutableStateOf<DapiDigimonResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }


    LaunchedEffect(decodedName) {
        isLoading = true
        digimon = getDigimonDetails(decodedName)
        isLoading = false
    }


    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        digimon?.let {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
            }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                if (it.images.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(it.images.first().href),
                        contentDescription = it.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Nombre: ${it.name}", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Nivel: ${it.levels.joinToString { lvl -> lvl.level }}")
                Text("Atributo: ${it.attributes.joinToString { attr -> attr.attribute }}")
                Text("Tipo: ${it.types.joinToString { t -> t.type }}")
                Text("Grupos: ${it.fields.joinToString { f -> f.field }}")
                Text("Fecha de salida: ${it.releaseDate ?: "Desconocida"}")

                val englishDescription = it.descriptions.firstOrNull { desc ->
                    desc.language.equals("en_us", ignoreCase = true)
                }?.description

                Spacer(modifier = Modifier.height(12.dp))
                Text("Descripción:", fontWeight = FontWeight.SemiBold)
                Text(englishDescription ?: "No disponible.")
            }

        } ?: Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No se encontraron detalles para $decodedName")
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