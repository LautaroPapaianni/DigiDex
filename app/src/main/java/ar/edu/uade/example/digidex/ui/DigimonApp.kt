package ar.edu.uade.example.digidex.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.uade.example.digidex.viewmodel.DigimonViewModel
import ar.edu.uade.example.digidex.data.model.Digimon
import ar.edu.uade.example.digidex.ui.screen.auth.AuthScreen
import ar.edu.uade.example.digidex.ui.screen.detail.DigimonDetailScreen
import ar.edu.uade.example.digidex.ui.screen.list.MainListScreen
import ar.edu.uade.example.digidex.ui.screen.splash.SplashScreen
import ar.edu.uade.example.digidex.viewmodel.DigimonViewModelFactory
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DigimonApp() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    var isAuthenticated by remember { mutableStateOf(auth.currentUser != null) }
    val viewModel: DigimonViewModel = viewModel(
        factory = DigimonViewModelFactory(LocalContext.current.applicationContext as Application)
    )



    NavHost(navController = navController,
        startDestination = if (isAuthenticated) "splash" else "auth") {
        composable("auth") {
            AuthScreen(
                onLoginSuccess = { navController.navigate("splash") },
                viewModel = viewModel
            )
        }
        composable("splash") {
            SplashScreen(navController = navController, viewModel = viewModel)
        }
        composable("list") {
            MainListScreen(
                viewModel = viewModel,
                onClick = { digimon: Digimon ->
                    navController.navigate("detail/${digimon.name}")
                },
                onLogout = {
                    isAuthenticated = false
                    navController.navigate("auth") {
                        popUpTo("list") { inclusive = true }
                    }
                }
            )
        }

        composable("detail/{name}") { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            DigimonDetailScreen(name = name, viewModel = viewModel, navController = navController)
        }

    }
}

