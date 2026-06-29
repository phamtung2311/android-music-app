package com.example.zingmp3

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.zingmp3.ui.screens.*
import com.example.zingmp3.ui.viewmodel.MusicViewModel
import com.example.zingmp3.ui.viewmodel.PlaylistViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        val role = sharedPref.getString("role", "user")

        setContent {
            val navController = rememberNavController()
            val musicViewModel: MusicViewModel = viewModel()
            val playlistViewModel: PlaylistViewModel = viewModel()

            val startDest = if (!isLoggedIn) {
                "welcome"
            } else if (role == "admin") {
                "admin"
            } else {
                "main_flow"
            }

            NavHost(
                navController = navController,
                startDestination = startDest
            ) {
                composable("welcome") {
                    WelcomeScreen(navController)
                }

                composable("login_flow") {
                    LoginScreen(navController)
                }

                composable("register") {
                    RegisterScreen(navController)
                }

                composable("main_flow") {
                    MainScreen(rememberNavController(), musicViewModel, playlistViewModel)
                }

                composable("admin") {
                    AdminScreen(navController)
                }
            }
        }
    }
}
