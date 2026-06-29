package com.example.zingmp3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            if (isLoading && playlistDetail == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF1DB954))
            } else if (error != null && playlistDetail == null) {
                Text(text = error ?: "Error", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            } else if (playlistDetail != null) {
                val songs = playlistDetail?.songs ?: emptyList()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        PlaylistHeader(
                            name = playlistDetail?.name ?: "",
                            imageUrl = if (songs.isNotEmpty()) songs[0].getFullImageUrl() else null,
                            onBack = { navController.popBackStack() },
                            onShufflePlay = {
                                if (songs.isNotEmpty()) {
                                    // Shuffle list and play from the first song of the shuffled list
                                    musicViewModel.playPlaylist(songs.shuffled(), 0)
                                    navController.navigate("player")
                                }
                            }
                        )
                    }

                    if (songs.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(text = "Chưa có bài hát nào trong playlist này", color = Color.Gray)
                            }
                        }
                    } else {
                        itemsIndexed(songs) { index, song: Song ->
                            PlaylistSongItem(
                                song = song,
                                onPlay = {
                                    // Load the whole playlist starting from this index
                                    musicViewModel.playPlaylist(songs, index)
                                    navController.navigate("player")
                                },
                                onDelete = {
                                    playlistViewModel.removeSongFromPlaylist(playlistId, song.id)
                                }
                            )
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun PlaylistHeader(
    name: String,
    imageUrl: String?,
    onBack: () -> Unit,
    onShufflePlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1DB954).copy(alpha = 0.3f), Color.Black),
                    startY = 0f,
                    endY = 500f
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.size(200.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.MusicNote)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = name,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = "Tạo bởi bạn • Muzic",
            color = Color.Gray,
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onShufflePlay,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
            shape = CircleShape,
            modifier = Modifier.height(56.dp).padding(horizontal = 32.dp)
        ) {
            Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("PHÁT NGẪU NHIÊN", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PlaylistSongItem(song: Song, onPlay: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
            Text(text = song.title ?: "Không rõ tiêu đề", color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(text = song.artist_name ?: "Không rõ nghệ sĩ", color = Color.Gray, fontSize = 12.sp, maxLines = 1)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
        }
    }
}
