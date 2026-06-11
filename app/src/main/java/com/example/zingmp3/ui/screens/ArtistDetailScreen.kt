package com.example.zingmp3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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
import com.example.zingmp3.network.model.Artist
import com.example.zingmp3.network.model.Song
import com.example.zingmp3.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    navController: NavController,
    artistId: Int,
    musicViewModel: MusicViewModel
) {
    val artists by musicViewModel.artists.collectAsState()
    val artist = remember(artists, artistId) { artists.find { it.id == artistId } }
    val songs by musicViewModel.artistSongs.collectAsState()
    
    val sortedSongs = remember(songs) {
        songs.sortedByDescending { it.releaseDate ?: "" }
    }

    LaunchedEffect(artistId) {
        musicViewModel.fetchArtistSongs(artistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artist?.stage_name ?: "Nghệ sĩ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageUrl = artist?.getFullAvatarUrl()
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = artist?.stage_name ?: "Unknown",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${artist?.followers_count ?: 0} người quan tâm",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            item {
                Text(
                    text = "Bài hát",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(sortedSongs) { song ->
                SongListItem(
                    song = song,
                    onClick = {
                        musicViewModel.playSong(song)
                        navController.navigate("player")
                    },
                    onMoreClick = { /* More options */ }
                )
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
