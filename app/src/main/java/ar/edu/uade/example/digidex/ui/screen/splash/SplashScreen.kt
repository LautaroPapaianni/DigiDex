package ar.edu.uade.example.digidex.ui.screen.splash

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ar.edu.uade.example.digidex.viewmodel.DigimonViewModel
import ar.edu.uade.example.digidex.R
import com.google.firebase.auth.FirebaseAuth


@Composable
fun SplashScreen(navController: NavController, viewModel: DigimonViewModel) {
    val context = LocalContext.current
    val frames = listOf(
        painterResource(R.drawable.huevo),
        painterResource(R.drawable.huevo_rotura),
        painterResource(R.drawable.huevo_eclosionando)
    )

    var frameIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(400L)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    // ⏳ Simulamos carga de 2.5 segundos y luego navegamos
    LaunchedEffect("navigate") {
        delay(2500L)

        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
        navController.navigate(if (isLoggedIn) "list" else "auth") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16164B))
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Image(
            painter = frames[frameIndex],
            contentDescription = null,
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.Center)
                .padding(top = 64.dp)
        )
    }
}

