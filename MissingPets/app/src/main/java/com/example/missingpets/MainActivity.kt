package com.example.missingpets

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.missingpets.ui.theme.MissingPetsTheme
import com.google.firebase.auth.FirebaseAuth


var authViewModel:AuthViewModel? = null

class MainActivity : ComponentActivity() {


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MissingPetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    val auth = FirebaseAuth.getInstance()
                    // Sign out the current user if any
                    auth.currentUser?.let {
                        auth.signOut()
                    }

                    authViewModel = viewModel()
                    val navController = rememberNavController()
                    val startDestination = Routes.LOGIN

                    NavHost(
                        navController,
                        startDestination = startDestination
                    ) {
                        composable(Routes.LOGIN) {
                            LoginScreen(authViewModel!!, navController)
                        }
                        composable(Routes.REGISTER) {
                            RegistrationScreen(authViewModel!!, navController)
                        }
                    }
                }
            }
        }
    }
}

