package ar.edu.uade.example.digidex

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


@Composable
fun DigimonDetailScreen(name: String, viewModel: DigimonViewModel, navController: NavController) {
    val decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8.toString())

    var digimon by remember { mutableStateOf<DapiDigimonResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }


    LaunchedEffect(decodedName) {
        isLoading = true
        digimon = viewModel.getDigimonDetails(decodedName)
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
                // El contenido principal (detalles)
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
                Text("Ubicación: ${it.fields.joinToString { f -> f.field }}")
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
