package ar.edu.uade.example.digidex.ui.screen.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ar.edu.uade.example.digidex.data.model.Digimon
import coil.compose.rememberAsyncImagePainter


@Composable
fun DigimonGridScreen(digimons: List<Digimon>, onClick: (Digimon) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
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
                Text(text = digimon.name, style = MaterialTheme.typography.bodySmall, color = (Color.White))
            }
        }
    }
}