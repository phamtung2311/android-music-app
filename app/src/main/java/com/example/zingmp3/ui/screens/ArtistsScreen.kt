package com.example.zingmp3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.zingmp3.network.model.Artist
import com.example.zingmp3.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(navController: NavController, musicViewModel: MusicViewModel) {
    val artists by musicViewModel.artists.collectAsState()
    val followedArtists by musicViewModel.followedArtists.collectAsState()
    
    var showOnlyFollowed by remember { mutableStateOf(false) }
    
    val displayArtists = if (showOnlyFollowed) followedArtists else artists

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tất cả nghệ sĩ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilterChip(
                        selected = showOnlyFollowed,
                        onClick = { showOnlyFollowed = !showOnlyFollowed },
                        label = { Text(if (showOnlyFollowed) "Đang theo dõi" else "Tất cả") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = Color.Gray,
                            selectedContainerColor = Color(0xFF1DB954).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF1DB954)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = showOnlyFollowed,
                            borderColor = Color.Gray,
                            selectedBorderColor = Color(0xFF1DB954)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
        if (displayArtists.isEmpty() && showOnlyFollowed) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Bạn chưa theo dõi nghệ sĩ nào", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(displayArtists, key = { it.id }) { artist ->
                    ArtistItem(artist) {
                        navController.navigate("artist_detail/${artist.id}")
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}
