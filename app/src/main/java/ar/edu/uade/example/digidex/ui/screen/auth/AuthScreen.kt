@file:Suppress("DEPRECATION")

package ar.edu.uade.example.digidex.ui.screen.auth

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ar.edu.uade.example.digidex.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import ar.edu.uade.example.digidex.viewmodel.DigimonViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit, viewModel: DigimonViewModel) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { authResult ->
                        if (authResult.isSuccessful) {
                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                            Log.d("AuthFlow", "Usuario $userId logueado. Iniciando carga de datos...")
                            val firestore = FirebaseFirestore.getInstance()

                            if (userId != null) {
                                val docRef = firestore.collection("users").document(userId)
                                docRef.get().addOnSuccessListener { document ->
                                    if (!document.exists()) {
                                        docRef.set(mapOf("favorites" to emptyList<String>()))
                                    }
                                    CoroutineScope(Dispatchers.Main).launch {
                                        Log.d("AuthScreen", "Login exitoso para $userId. UID de FirebaseAuth: ${FirebaseAuth.getInstance().currentUser?.uid}")
                                        Log.d("AuthFlow", "Llamando a loadDigimons()...")
                                        viewModel.loadDigimons()
                                        viewModel.inicializarSesion(userId)
                                        Log.d("AuthFlow", "loadDigimons() completado. Tamaño de digimonList: ${viewModel.digimonList.size}")
                                        if (viewModel.digimonList.isNotEmpty()) {
                                            Log.d("AuthFlow", "Llamando a loadFavoritesFromFirestore($userId)...")
                                            viewModel.loadFavoritesFromFirestore(userId)
                                            Log.d("AuthFlow", "loadFavoritesFromFirestore() llamado.")
                                        } else {
                                            Log.e("AuthFlow", "ERROR: digimonList sigue vacía después de loadDigimons(). No se pueden cargar favoritos.")
                                            // Aquí deberías manejar este error, quizás reintentar loadDigimons o mostrar un mensaje.
                                        }
                                        onLoginSuccess()
                                    }
                                }.addOnFailureListener { e ->
                                    Log.e("AuthScreen", "Error al verificar documento del usuario", e)
                                    Toast.makeText(context, "Error al verificar datos del usuario", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        else {
                            Log.e("AuthScreen", "Error Firebase", authResult.exception)
                            Toast.makeText(
                                context,
                                "Error al autenticar con Firebase",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                12501 -> {
                    Log.w("AuthScreen", "Inicio cancelado por el usuario (12501)")
                    Toast.makeText(context, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show()
                }
                12502 -> {
                    Log.e("AuthScreen", "Error interno (12502)", e)
                    Toast.makeText(context, "Error interno de inicio de sesión", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Log.e("AuthScreen", "Error inesperado: ${e.statusCode}", e)
                    Toast.makeText(context, "Error: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_login),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    launcher.launch(signInIntent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Ingresar con Google", color = Color.Black)
            }
        }
    }
}
