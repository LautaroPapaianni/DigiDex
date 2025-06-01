package ar.edu.uade.example.digidex

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainListScreen(viewModel: DigimonViewModel, onClick: (Digimon) -> Unit, onLogout: () -> Unit) {
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var sortOption by remember { mutableStateOf("A-Z") }
    var selectedLevel by remember { mutableStateOf("Todos") }

    val digimons = viewModel.digimonList
        .filter {
            it.name.contains(searchText.text, ignoreCase = true)
        }
        .filter {
            selectedLevel == "Todos" || it.level.equals(selectedLevel, ignoreCase = true)
        }
        .sortedWith(compareBy {
            when (sortOption) {
                "A-Z" -> it.name
                "Z-A" -> it.name.reversed()
                "Nivel ↑" -> it.level
                "Nivel ↓" -> it.level.reversed()
                else -> it.name
            }
        })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Buscar...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                actions = {
                    SortMenu { sortOption = it }
                    FilterMenu(viewModel.digimonList.map { it.level }.distinct()) {
                        selectedLevel = it
                    }
                    IconButton(onClick = { onLogout() }) {
                        Icon(painter = painterResource(id = R.drawable.ic_logout), contentDescription = "Cerrar sesión")
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
