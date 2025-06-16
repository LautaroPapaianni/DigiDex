package ar.edu.uade.example.digidex.ui.screen.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ar.edu.uade.example.digidex.R
import ar.edu.uade.example.digidex.data.model.Digimon
import coil.compose.rememberAsyncImagePainter

@Composable
fun DigimonGridScreen(
    digimons: List<Digimon>,
    onClick: (Digimon) -> Unit,
    onLongClick: (Digimon) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(digimons, key = { it.name }) { digimon ->
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick(digimon) },
                            onLongPress = { onLongClick(digimon) }
                        )
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Image(
                            painter = rememberAsyncImagePainter(digimon.img),
                            contentDescription = digimon.name,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray)
                        )
                        if (digimon.isFavorite) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_star),
                                contentDescription = "Favorito",
                                tint = Color.Yellow,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-4).dp, y = (-4).dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = digimon.name, style = MaterialTheme.typography.bodySmall, color = (Color.White))
                }
            }
        }
    }
}