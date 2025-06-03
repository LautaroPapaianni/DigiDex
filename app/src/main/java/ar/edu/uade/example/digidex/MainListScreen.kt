package ar.edu.uade.example.digidex

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
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

@Composable
fun DigimonGridScreen(digimons: List<Digimon>, onClick: (Digimon) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(digimons) { digimon ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onClick(digimon) }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(digimon.img),
                    contentDescription = digimon.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = digimon.name, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SortMenu(onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painter = painterResource(id = R.drawable.ic_sort), contentDescription = "Ordenar")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("A-Z", "Z-A", "Nivel ↑", "Nivel ↓").forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FilterMenu(levels: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painter = painterResource(id = R.drawable.ic_filter), contentDescription = "Filtrar")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Todos") },
                onClick = {
                    onSelect("Todos")
                    expanded = false
                }
            )
            levels.distinct().sorted().forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                )
            }
        }
    }
}
