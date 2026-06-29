package com.example.zingmp3.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.zingmp3.ui.viewmodel.MusicViewModel
import com.example.zingmp3.ui.viewmodel.PlaylistViewModel

@Composable
fun MainScreen(
    navController: NavHostController,
    musicViewModel: MusicViewModel = viewModel(),
    playlistViewModel: PlaylistViewModel = viewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()

    val bottomNavItems = listOf(
        "home" to ("Trang chủ" to Icons.Filled.Home),
        "search" to ("Tìm kiếm" to Icons.Filled.Search),
        "library" to ("Thư viện" to Icons.Filled.LibraryMusic),
        "premium" to ("Premium" to Icons.Filled.WorkspacePremium)
    )

    val showBottomBar = currentRoute in listOf("home", "search", "library", "premium")

    Scaffold(
        bottomBar = {
            if (showBottomBar || currentSong != null) {
                Column {
                    currentSong?.let { song ->
                        if (currentRoute != "player") {
                            NowPlayingBar(
                                song = song,
                                isPlaying = isPlaying,
                                onTogglePlay = musicViewModel::togglePlayPause,
                                onClose = musicViewModel::stopAndClear,
                                onClick = { navController.navigate("player") },
                                onArtistClick = { artistId ->
                                    navController.navigate("artist_detail/$artistId")
                                }
                            )
                        }
                    }
                    if (showBottomBar) {
                        NavigationBar(containerColor = Color.Black, contentColor = Color.White) {
                            bottomNavItems.forEach { (route, pair) ->
                                val (label, icon) = pair
                                NavigationBarItem(
                                    icon = { Icon(icon, label) },
                                    label = { Text(label) },
                                    selected = currentRoute == route,
                                    onClick = {
                                        if (currentRoute != route) {
                                            navController.navigate(route) {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF1DB954),
                                        selectedTextColor = Color(0xFF1DB954),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomeScreen(navController, musicViewModel, playlistViewModel)
                }
                composable("search") {
                    SearchScreen(navController, musicViewModel, playlistViewModel)
                }
                composable("library") {
                    LibraryScreen(navController, playlistViewModel, musicViewModel)
                }
                composable("premium") {
                    // Placeholder for Premium screen
                    Box { Text("Premium Screen", color = Color.White) }
                }
                composable(
                    "playlist_detail/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getInt("playlistId") ?: 0
                    PlaylistDetailScreen(navController, playlistId, playlistViewModel, musicViewModel)
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
                    ArtistDetailScreen(navController, artistId, musicViewModel, playlistViewModel)
                }
                composable("settings") {
                    SettingsScreen(navController)
                }
            }
        }
    }
}
