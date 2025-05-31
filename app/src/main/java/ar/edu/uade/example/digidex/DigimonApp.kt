package ar.edu.uade.example.digidex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun DigimonApp() {
    val navController = rememberNavController()
    val viewModel = remember { DigimonViewModel() }

    NavHost(navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController, viewModel)
        }
        composable("list") {
            DigimonGridScreen(viewModel.digimonList) { digimon ->
                navController.navigate("detail/${digimon.name}")
            }
        }
        composable("detail/{name}") { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            DigimonDetailScreen(name = name, viewModel = viewModel, navController = navController)
        }
    }
}

