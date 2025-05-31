package ar.edu.uade.example.digidex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp



@Composable
fun SplashScreen(navController: NavController, viewModel: DigimonViewModel) {
    val frames = listOf(
        painterResource(R.drawable.huevo),
        painterResource(R.drawable.huevo_rotura),
        painterResource(R.drawable.huevo_eclosionando)
    )

    var frameIndex by remember { mutableStateOf(0) }

    // Avanza cada 500ms al siguiente frame
    LaunchedEffect(Unit) {
        while (viewModel.isLoading) {
            delay(500)
            frameIndex = (frameIndex + 1) % frames.size
        }
        // Cuando termine de cargar, ir al listado
        navController.navigate("list") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16164B)), // fondo azul oscuro
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.splash_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Animación central de los huevos
        Image(
            painter = frames[frameIndex],
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        )
    }
}
