package ar.edu.uade.example.digidex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DigimonApp() {
    val navController = rememberNavController()
    var isAuthenticated by remember { mutableStateOf(false) }
    val viewModel: DigimonViewModel = viewModel()


    NavHost(navController = navController,
        startDestination = if (isAuthenticated) "list" else "auth") {
        composable("auth") {
            AuthScreen {
                isAuthenticated = true
                navController.navigate("list") {
                    popUpTo("auth") { inclusive = true }
                }
            }
        }
        composable("splash") {
            SplashScreen(navController, viewModel)
        }
        composable("list") {
            MainListScreen(
                viewModel = viewModel,
                onClick = { digimon: Digimon ->  // <--- Usa "onClick", no "onItemClick"
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

