package com.example.zingmp3.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.zingmp3.ui.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    navController: NavController,
    artistId: Int,
    musicViewModel: MusicViewModel,
    playlistViewModel: PlaylistViewModel
) {
    val artists by musicViewModel.artists.collectAsState()
    val artist = remember(artists, artistId) { artists.find { it.id == artistId } }
    val songs by musicViewModel.artistSongs.collectAsState()
    val isFollowed by musicViewModel.isArtistFollowed.collectAsState()
    val isBuffering by musicViewModel.isBuffering.collectAsState()
    
    var songForOptions by remember { mutableStateOf<Song?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    
    val popularSongs = remember(songs) {
        songs.sortedByDescending { it.views }.take(5)
    }

    LaunchedEffect(artistId) {
        musicViewModel.fetchArtistSongs(artistId)
        musicViewModel.checkFollowStatus(artistId)
    }

    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            item {
                ArtistHeader(
                    artist = artist,
                    isFollowed = isFollowed,
                    onBack = { navController.popBackStack() },
                    onFollowClick = { musicViewModel.followArtist(artistId) },
                    onPlayAll = {
                        if (songs.isNotEmpty()) {
                            musicViewModel.playPlaylist(songs, 0)
                            navController.navigate("player")
                        }
                    }
                )
            }

            if (popularSongs.isNotEmpty()) {
                item {
                    SectionTitle("Bài hát nổi bật")
                }

                itemsIndexed(popularSongs) { index, song: Song ->
                    SongListItem(
                        song = song,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = {
                            musicViewModel.playPlaylist(popularSongs, index)
                            navController.navigate("player")
                        },
                        onMoreClick = { songForOptions = song }
                    )
                }
            }

            if (songs.isNotEmpty()) {
                item {
                    SectionTitle("Tất cả bài hát")
                }

                itemsIndexed(songs) { index, song: Song ->
                    SongListItem(
                        song = song,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = {
                            musicViewModel.playPlaylist(songs, index)
                            navController.navigate("player")
                        },
                        onMoreClick = { songForOptions = song }
                    )
                }
            }

            if (songs.isEmpty() && !isBuffering) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Nghệ sĩ chưa có bài hát nào", color = Color.Gray)
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    songForOptions?.let { song ->
        SongOptionsBottomSheet(
            song = song,
            musicViewModel = musicViewModel,
            playlistViewModel = playlistViewModel,
            onDismiss = { songForOptions = null },
            onAddToPlaylist = {
                songForOptions = null
                songToAddToPlaylist = song
            }
        )
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
fun ArtistHeader(
    artist: Artist?,
    isFollowed: Boolean,
    onBack: () -> Unit,
    onFollowClick: () -> Unit,
    onPlayAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        AsyncImage(
            model = artist?.getFullAvatarUrl(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 300f
                    )
                )
        )
        
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 40.dp, start = 8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = artist?.stage_name ?: "",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${artist?.followers_count ?: 0} người quan tâm",
                color = Color.LightGray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onFollowClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowed) Color.Transparent else Color(0xFF1DB954)
                    ),
                    border = if (isFollowed) BorderStroke(1.dp, Color.White) else null,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isFollowed) "Bỏ theo dõi" else "Quan tâm")
                }
                Spacer(modifier = Modifier.width(16.dp))
                FloatingActionButton(
                    onClick = onPlayAll,
                    containerColor = Color(0xFF1DB954),
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play All")
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.getFullImageUrl(),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title ?: "", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = song.artist_name ?: "", color = Color.Gray, fontSize = 14.sp)
        }
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
        }
    }
}
