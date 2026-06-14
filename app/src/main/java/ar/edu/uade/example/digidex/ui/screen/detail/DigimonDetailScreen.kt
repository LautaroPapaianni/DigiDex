package ar.edu.uade.example.digidex.ui.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite // Para el botón de favorito
import androidx.compose.material.icons.filled.FavoriteBorder // Para el botón de favorito
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ar.edu.uade.example.digidex.data.db.DigimonDatabase
import ar.edu.uade.example.digidex.data.model.Digimon // Modelo de UI para pasar a toggleFavorite
import ar.edu.uade.example.digidex.viewmodel.DigimonDetailViewModel
import ar.edu.uade.example.digidex.viewmodel.DigimonDetailViewModelFactory
import ar.edu.uade.example.digidex.viewmodel.DigimonViewModel // El ViewModel principal
import coil.compose.rememberAsyncImagePainter
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigimonDetailScreen(
    digimonNameArg: String, // El nombre que llega de la navegación
    navController: NavController,
    mainViewModel: DigimonViewModel // Inyecta el ViewModel principal
) {
    val decodedName = remember(digimonNameArg) {
        URLDecoder.decode(digimonNameArg, StandardCharsets.UTF_8.toString())
    }

    val context = LocalContext.current
    val dao = DigimonDatabase.getInstance(context.applicationContext).digimonDao()

    // Usar la lista de nombres de favoritos del ViewModel principal
    // Si mainViewModel.favoriteDigimonNames es un StateList, Compose lo observará.
    val favoriteNamesFromMainVM = mainViewModel.favoriteDigimonNames

    val detailViewModel: DigimonDetailViewModel = viewModel(
        factory = DigimonDetailViewModelFactory(decodedName, dao, favoriteNamesFromMainVM)
    )

    val uiState by rememberUpdatedState(detailViewModel.uiState) // O usa collectAsState si uiState es un Flow

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.digimonData?.name ?: decodedName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón de Favorito
                    uiState.digimonData?.let { digimonEntity ->
                        // Botón de Favorito corregido
                        uiState.digimonData?.let {
                            IconButton(onClick = {
                                // Llamamos a una nueva función en el ViewModel de detalle
                                detailViewModel.toggleFavorite(mainViewModel)
                            }) {
                                Icon(
                                    imageVector = if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorito",
                                    // Cambié el color a Rojo para que se note más el cambio, pero puedes dejar el primary
                                    tint = if (uiState.isFavorite) androidx.compose.ui.graphics.Color.Red else LocalContentColor.current
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center // Centra el CircularProgressIndicator y el mensaje de error
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else if (uiState.digimonData != null) {
                val digimon = uiState.digimonData // Esta es tu DigimonEntity con detalles de DAPI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()) // Para contenido largo
                ) {
                    // Imagen principal (de DAPI si está, sino la de la lista inicial)
                    val imageUrl = digimon?.dapiImages?.firstOrNull() ?: digimon?.img
                    if (!imageUrl.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUrl),
                            contentDescription = digimon?.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .align(Alignment.CenterHorizontally) // Centrar imagen
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text("Nombre: ${digimon?.name}", fontWeight = FontWeight.Bold, fontSize = 22.sp)

                    // Nivel (de DAPI si está, sino el de la lista inicial)
                    val levelText = digimon?.dapiLevels?.joinToString(", ") ?: digimon?.level
                    if (!levelText.isNullOrBlank()) {
                        Text("Nivel: $levelText")
                    }

                    // Atributo (de DAPI)
                    digimon?.dapiAttributes?.let { attributes ->
                        if (attributes.isNotEmpty()) {
                            Text("Atributo: ${attributes.joinToString(", ")}")
                        }
                    }

                    // Tipo (de DAPI)
                    digimon?.dapiTypes?.let { types ->
                        if (types.isNotEmpty()) {
                            Text("Tipo: ${types.joinToString(", ")}")
                        }
                    }

                    // Grupos/Fields (de DAPI)
                    digimon?.dapiFields?.let { fields ->
                        if (fields.isNotEmpty()) {
                            Text("Grupos: ${fields.joinToString(", ")}")
                        }
                    }

                    // Año de primera aparición (de DAPI)
                    digimon?.dapiReleaseDate?.let {
                        Text("Año de primera aparición: $it")
                    }

                    // Descripción (de DAPI)
                    digimon?.dapiDescription?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Descripción:", fontWeight = FontWeight.SemiBold)
                        Text(it)
                    } ?: Text("Descripción no disponible.")

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Detalles cargados de DAPI: ${digimon?.detailsFetchedFromDapi}", fontSize = 12.sp)


                }
            } else {
                Text("No se encontraron detalles para $decodedName", modifier = Modifier.padding(16.dp))
            }
        }
    }
}