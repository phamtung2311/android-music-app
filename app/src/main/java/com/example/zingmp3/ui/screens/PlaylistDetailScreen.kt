package com.example.zingmp3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.zingmp3.network.model.Song
import com.example.zingmp3.ui.viewmodel.MusicViewModel
import com.example.zingmp3.ui.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    navController: NavController,
    playlistId: Int,
    playlistViewModel: PlaylistViewModel,
    musicViewModel: MusicViewModel
) {
    val playlistDetail by playlistViewModel.playlistDetail.collectAsState()
    val isLoading by playlistViewModel.isLoading.collectAsState()
    val error by playlistViewModel.error.collectAsState()

    LaunchedEffect(playlistId) {
        playlistViewModel.fetchPlaylistDetails(playlistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(playlistDetail?.name ?: "Playlist", fontWeight = FontWeight.Bold)
                        playlistDetail?.let {
                            Text("${it.songs.size} bài hát", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Normal)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && playlistDetail == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF1DB954))
            } else if (error != null && playlistDetail == null) {
                Text(text = error ?: "Error", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            } else if (playlistDetail != null) {
                val songs = playlistDetail?.songs ?: emptyList()
                if (songs.isEmpty()) {
                    Text(text = "Chưa có bài hát nào trong playlist này", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(songs) { song ->
                            PlaylistSongItem(
                                song = song,
                                onPlay = {
                                    musicViewModel.playSong(song)
                                    navController.navigate("player")
                                },
                                onDelete = {
                                    playlistViewModel.removeSongFromPlaylist(playlistId, song.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistSongItem(song: Song, onPlay: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onPlay() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.getFullImageUrl(),
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title ?: "Unknown", color = Color.White, fontWeight = FontWeight.Medium)
            Text(text = song.artist_name ?: "Unknown", color = Color.Gray, fontSize = 12.sp)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Gray)
        }
    }
}
