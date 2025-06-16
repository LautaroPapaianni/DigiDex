package ar.edu.uade.example.digidex.ui.screen.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import ar.edu.uade.example.digidex.viewmodel.DigimonViewModel
import ar.edu.uade.example.digidex.data.model.Digimon
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainListScreen(
    viewModel: DigimonViewModel,
    onClick: (Digimon) -> Unit,
    onLogout: () -> Unit
) {
    val searchText = viewModel.searchText
    val sortOption = viewModel.sortOption
    val selectedLevel = viewModel.selectedLevel
    var showDialog by remember { mutableStateOf(false) }
    var selectedDigimon by remember { mutableStateOf<Digimon?>(null) }

    val digimons = viewModel.digimonList
        .filter { it.name.contains(viewModel.searchText, ignoreCase = true) }
        .filter { viewModel.selectedLevel == "Todos" || it.level.equals(viewModel.selectedLevel, ignoreCase = true) }
        .sortedWith(
            compareByDescending<Digimon> { it.isFavorite }.thenBy {
                when (viewModel.sortOption) {
                    "A-Z" -> it.name
                    "Z-A" -> it.name.reversed()
                    "Nivel ↑" -> it.level
                    "Nivel ↓" -> it.level.reversed()
                    else -> it.name
                }
            }
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = viewModel.searchText,
                        onValueChange = { viewModel.searchText = it },
                        placeholder = { Text("Buscar...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                actions = {
                    SortMenu { viewModel.sortOption = it }
                    FilterMenu(viewModel.digimonList.map { it.level }.distinct()) {
                        viewModel.selectedLevel = it
                    }
                    var expanded by remember { mutableStateOf(false) }

                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val context = LocalContext.current

                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            onClick = {
                                expanded = false
                                viewModel.logout(context) {
                                    onLogout()
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Salir de la app") },
                            onClick = {
                                expanded = false
                                exitProcess(0)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF8C00)
                )
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF253C86))
        ) {
            DigimonGridScreen(
                digimons = digimons,
                onClick = onClick,
                onLongClick = {
                    selectedDigimon = it
                    showDialog = true
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
    if (showDialog && selectedDigimon != null) {

        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDigimon?.name?.let {
                        viewModel.toggleFavorite(it)
//                        refreshKey++
                    }
                    showDialog = false
                }) {
                    Text(if (selectedDigimon!!.isFavorite) "Quitar" else "Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text(if (selectedDigimon!!.isFavorite) "Quitar de favoritos" else "Agregar a favoritos") },
            text = {
                Text(
                    if (selectedDigimon!!.isFavorite)
                        "¿Querés quitar a ${selectedDigimon?.name} de tus favoritos?"
                    else
                        "¿Querés agregar a ${selectedDigimon?.name} a tus favoritos?"
                )
            }
        )
    }
}