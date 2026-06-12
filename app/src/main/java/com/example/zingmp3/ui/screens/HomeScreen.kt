package com.example.zingmp3.ui.screens

import android.content.Context.MODE_PRIVATE
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.zingmp3.network.model.Artist
import com.example.zingmp3.network.model.Song
import com.example.zingmp3.ui.viewmodel.MusicViewModel
import com.example.zingmp3.ui.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController, 
    musicViewModel: MusicViewModel = viewModel(),
    playlistViewModel: PlaylistViewModel = viewModel()
) {
    val username by musicViewModel.username.collectAsState()

    val songs by musicViewModel.songs.collectAsState()
    val recommendedSongs by musicViewModel.recommendedSongs.collectAsState()
    val recommendedArtists by musicViewModel.recommendedArtists.collectAsState()
    val top20Songs by musicViewModel.top20WeeklySongs.collectAsState()
    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val selectedGenre by musicViewModel.selectedGenre.collectAsState()
    val genres by musicViewModel.genres.collectAsState()

    var selectedItem by remember { mutableIntStateOf(0) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    
    val items = remember { listOf("Home", "Search", "Library", "Premium") }
    val icons = remember { listOf(Icons.Filled.Home, Icons.Filled.Search, Icons.Filled.LibraryMusic, Icons.Filled.WorkspacePremium) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            selectedItem = 0
                            musicViewModel.setGenre("All")
                        }
                    ) {
                        Icon(Icons.Filled.LibraryMusic, null, tint = Color(0xFF1DB954), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Muzic", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Filled.Notifications, null) }
                    IconButton(onClick = { navController.navigate("profile") }) { Icon(Icons.Filled.AccountCircle, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White, actionIconContentColor = Color.White)
            )
        },
        bottomBar = {
            Column {
                currentSong?.let { song ->
                    NowPlayingBar(
                        song = song, 
                        isPlaying = isPlaying, 
                        onTogglePlay = musicViewModel::togglePlayPause, 
                        onClick = { navController.navigate("player") },
                        onArtistClick = { artistId ->
                            navController.navigate("artist_detail/$artistId")
                        }
                    )
                }
                NavigationBar(containerColor = Color.Black, contentColor = Color.White) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(icons[index], item) },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF1DB954), selectedTextColor = Color(0xFF1DB954), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent)
                        )
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        if (selectedItem == 2) {
            Box(modifier = Modifier.padding(padding)) {
                LibraryScreen(navController, playlistViewModel, musicViewModel)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(genres, key = { it }) { category ->
                            val isSelected = selectedGenre == category
                            SuggestionChip(
                                onClick = { musicViewModel.setGenre(category) },
                                label = { Text(category, color = if (isSelected) Color.Black else Color.White) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) Color(0xFF1DB954) else Color.DarkGray.copy(alpha = 0.5f)
                                ),
                                border = null,
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }

                // Section: BXH Top 20 Weekly
                if (selectedGenre == "All" && top20Songs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = "BXH Nhạc Mới (Tuần này)", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Khung cuộn riêng cho BXH
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp) // Chiều cao tương đương ~5 bài RankingItem
                                .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(top20Songs, key = { _, song -> "top_scroll_${song.id}" }) { index, song ->
                                    RankingItem(
                                        rank = index + 1, 
                                        song = song, 
                                        onClick = {
                                            musicViewModel.playSong(song)
                                            navController.navigate("player")
                                        }, 
                                        onMoreClick = { songToAddToPlaylist = song },
                                        onArtistClick = { artistId ->
                                            navController.navigate("artist_detail/$artistId")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedGenre == "All") {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = "Gợi ý cho $username", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(recommendedSongs, key = { it.id }) { song ->
                                SongCard(
                                    song = song, 
                                    onClick = { 
                                        musicViewModel.playSong(song)
                                        navController.navigate("player")
                                    },
                                    onArtistClick = { artistId ->
                                        navController.navigate("artist_detail/$artistId")
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Nghệ sĩ phổ biến", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Xem tất cả",
                                color = Color(0xFF1DB954),
                                modifier = Modifier.clickable { navController.navigate("artists") }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(recommendedArtists, key = { it.id }) { artist ->
                                ArtistItem(artist) {
                                    navController.navigate("artist_detail/${artist.id}")
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (selectedGenre == "All") "Mới phát hành" else "Top bài hát $selectedGenre", 
                        color = Color.White, 
                        fontSize = 22.sp, 
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Khung cuộn riêng cho danh sách bài hát mới
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp) // Chiều cao cố định tương đương ~5 bài hát
                            .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(songs, key = { "recent_scroll_${it.id}" }) { song ->
                                SongListItem(
                                    song = song,
                                    onClick = {
                                        musicViewModel.playSong(song)
                                        navController.navigate("player")
                                    },
                                    onMoreClick = { songToAddToPlaylist = song },
                                    onArtistClick = { artistId ->
                                        navController.navigate("artist_detail/$artistId")
                                    }
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    songToAddToPlaylist?.let { song ->
        AddToPlaylistBottomSheet(
            song = song,
            viewModel = playlistViewModel,
            onDismiss = { songToAddToPlaylist = null }
        )
    }
}

@Composable
fun RankingItem(rank: Int, song: Song, onClick: () -> Unit, onMoreClick: () -> Unit, onArtistClick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            color = when(rank) {
                1 -> Color(0xFF4A90E2)
                2 -> Color(0xFF50E3C2)
                3 -> Color(0xFFF5A623)
                else -> Color.Gray
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
        AsyncImage(model = song.getFullImageUrl(), contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title ?: "Unknown Title", color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                text = song.artist_name ?: "Unknown", 
                color = Color.Gray, 
                fontSize = 12.sp,
                modifier = Modifier.clickable { song.artist_id?.let { onArtistClick(it) } }
            )
        }
        Text(text = "${song.views} views", color = Color.DarkGray, fontSize = 11.sp)
        IconButton(onClick = onMoreClick) { Icon(Icons.Filled.MoreVert, null, tint = Color.Gray) }
    }
}

@Composable
fun SongCard(song: Song, onClick: () -> Unit, onArtistClick: (Int) -> Unit) {
    Column(modifier = Modifier.width(150.dp).clickable { onClick() }) {
        AsyncImage(model = song.getFullImageUrl(), contentDescription = song.title ?: "Song Cover", modifier = Modifier.size(150.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = song.title ?: "Unknown Title", color = Color.White, maxLines = 1, fontWeight = FontWeight.SemiBold)
        Text(
            text = song.artist_name ?: "Unknown", 
            color = Color.Gray, 
            fontSize = 12.sp, 
            maxLines = 1,
            modifier = Modifier.clickable { song.artist_id?.let { onArtistClick(it) } }
        )
    }
}

@Composable
fun ArtistItem(artist: Artist, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(120.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            val imageUrl = artist.getFullAvatarUrl()
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = artist.stage_name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
            }

            // Nhãn hiển thị số người quan tâm
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val followerText = when {
                        artist.followers_count >= 1000000 -> String.format("%.1fM", artist.followers_count / 1000000f)
                        artist.followers_count >= 1000 -> String.format("%.1fK", artist.followers_count / 1000f)
                        else -> artist.followers_count.toString()
                    }
                    Text(
                        text = followerText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = artist.stage_name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun SongListItem(song: Song, onClick: () -> Unit, onMoreClick: () -> Unit, onArtistClick: (Int) -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = song.getFullImageUrl(), contentDescription = song.title ?: "Song Cover", modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title ?: "Unknown Title", color = Color.White, fontWeight = FontWeight.Medium)
            Text(
                text = song.artist_name ?: "Unknown", 
                color = Color.Gray, 
                fontSize = 12.sp,
                modifier = Modifier.clickable { song.artist_id?.let { onArtistClick(it) } }
            )
        }
        IconButton(onClick = onMoreClick) { Icon(Icons.Filled.MoreVert, null, tint = Color.Gray) }
    }
}

@Composable
fun NowPlayingBar(song: Song, isPlaying: Boolean, onTogglePlay: () -> Unit, onClick: () -> Unit, onArtistClick: (Int) -> Unit) {
    Surface(color = Color(0xFF282828), modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp, vertical = 4.dp).clip(RoundedCornerShape(8.dp)).clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            AsyncImage(model = song.getFullImageUrl(), contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title ?: "Unknown Title", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    text = song.artist_name ?: "Unknown", 
                    color = Color.Gray, 
                    fontSize = 12.sp, 
                    maxLines = 1,
                    modifier = Modifier.clickable { song.artist_id?.let { onArtistClick(it) } }
                )
            }
            IconButton(onClick = onTogglePlay) { Icon(imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White) }
        }
    }
}
