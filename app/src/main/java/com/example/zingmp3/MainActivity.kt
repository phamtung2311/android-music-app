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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        val role = sharedPref.getString("role", "user")

        setContent {
            val navController = rememberNavController()
            val musicViewModel: MusicViewModel = viewModel()
            val playlistViewModel: PlaylistViewModel = viewModel()

            val startDest = if (!isLoggedIn) {
                "login"
            } else if (role == "admin") {
                "admin"
            } else {
                "home"
            }

            NavHost(
                navController = navController,
                startDestination = startDest
            ) {
                composable("login") {
                    LoginScreen(navController)
                }

                composable("register") {
                    RegisterScreen(navController)
                }

                composable("home") {
                    HomeScreen(navController, musicViewModel, playlistViewModel)
                }

                composable(
                    "playlist_detail/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getInt("playlistId") ?: 0
                    PlaylistDetailScreen(navController, playlistId, playlistViewModel, musicViewModel)
                }

                composable("admin") {
                    AdminScreen(navController)
                }

                composable("player") {
                    PlayerScreen(navController, musicViewModel, playlistViewModel)
                }

                composable("profile") {
                    ProfileScreen(navController)
                }

                composable("artists") {
                    ArtistsScreen(navController, musicViewModel)
                }

                composable(
                    "artist_detail/{artistId}",
                    arguments = listOf(navArgument("artistId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val artistId = backStackEntry.arguments?.getInt("artistId") ?: 0
                    ArtistDetailScreen(navController, artistId, musicViewModel)
                }
            }
        }
    }
}
