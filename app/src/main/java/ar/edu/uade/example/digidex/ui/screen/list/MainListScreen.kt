package ar.edu.uade.example.digidex.ui.screen.list

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    val digimons = viewModel.digimonList
        .filter {
            it.name.contains(viewModel.searchText, ignoreCase = true)
        }
        .filter {
            viewModel.selectedLevel == "Todos" || it.level.equals(viewModel.selectedLevel, ignoreCase = true)
        }
        .let { list ->
            when (viewModel.sortOption) {
                "A-Z" -> list.sortedBy { it.name }
                "Z-A" -> list.sortedByDescending { it.name }
                "Nivel ↑" -> list.sortedBy { it.level }
                "Nivel ↓" -> list.sortedByDescending { it.level }
                else -> list
            }
        }

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
                                exitProcess(0) // Cierra la app
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        DigimonGridScreen(digimons = digimons, onClick = onClick, modifier = Modifier.padding(padding))
    }
}

