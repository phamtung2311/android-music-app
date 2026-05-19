package com.example.zingmp3.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.zingmp3.ui.viewmodel.MusicViewModel
import java.util.concurrent.TimeUnit

@Composable
fun PlayerScreen(navController: NavController, musicViewModel: MusicViewModel) {
    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()

    val backgroundBrush = remember {
        Brush.verticalGradient(listOf(Color(0xFF333333), Color.Black))
    }

    currentSong?.let { song ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerHeader(
                onBack = { navController.popBackStack() },
                onAddFavorite = { musicViewModel.favoriteSong(song.id) }
            )

            Spacer(modifier = Modifier.weight(0.5f))

            // Đĩa quay cô lập, không bị ảnh hưởng bởi các thành phần khác
            RotatingCD(imageUrl = song.getFullImageUrl(), isPlaying = isPlaying)

            Spacer(modifier = Modifier.weight(0.5f))

            // Thông tin lượt xem cô lập
            SongStatsRow(song.views, song.likes_count)

            Spacer(modifier = Modifier.height(16.dp))

            SongInfo(title = song.title, artist = song.artist_name ?: "Unknown Artist")

            Spacer(modifier = Modifier.height(24.dp))

            // Nút Tim cô lập hoàn toàn
            LikeSection(musicViewModel, song.id)

            Spacer(modifier = Modifier.height(24.dp))

            PlaybackProgress(musicViewModel)

            Spacer(modifier = Modifier.height(24.dp))

            PlayerControls(
                isPlaying = isPlaying,
                onTogglePlay = { musicViewModel.togglePlayPause() },
                onSeekForward = { musicViewModel.seekTo(musicViewModel.currentPosition.value + 10000) },
                onSeekBackward = { musicViewModel.seekTo(musicViewModel.currentPosition.value - 10000) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SongStatsRow(views: Int, likes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Headset, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Text(text = " $views  •  ", color = Color.Gray, fontSize = 14.sp)
        Icon(Icons.Default.Favorite, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Text(text = " $likes", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun LikeSection(viewModel: MusicViewModel, songId: Int) {
    val isLiked by viewModel.isCurrentLiked.collectAsState()
    val context = LocalContext.current
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        IconButton(
            onClick = { 
                viewModel.likeSong(songId) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (isLiked) Color.Red else Color.White,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
fun RotatingCD(imageUrl: String?, isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "CDRotation")
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    val painter = rememberAsyncImagePainter(model = imageUrl)

    Box(
        modifier = Modifier
            .size(280.dp)
            .graphicsLayer {
                rotationZ = if (isPlaying) rotation.value else 0f
            }
            .clip(CircleShape)
            .background(Color.Black)
    ) {
        Image(painter = painter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.size(40.dp).align(Alignment.Center).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackProgress(musicViewModel: MusicViewModel) {
    val position by musicViewModel.currentPosition.collectAsState()
    val duration by musicViewModel.duration.collectAsState()
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(position, duration) {
        if (!isDragging && duration > 0) {
            sliderPosition = position.toFloat() / duration.toFloat()
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Slider(
            value = sliderPosition,
            onValueChange = { isDragging = true; sliderPosition = it },
            onValueChangeFinished = {
                isDragging = false
                musicViewModel.seekTo((sliderPosition * duration).toLong())
            },
            colors = SliderDefaults.colors(thumbColor = Color(0xFF1DB954), activeTrackColor = Color(0xFF1DB954), inactiveTrackColor = Color.White.copy(alpha = 0.2f)),
            thumb = {
                Box(modifier = Modifier.size(16.dp).background(Color(0xFF1DB954), CircleShape).graphicsLayer {
                    scaleX = if (isDragging) 1.5f else 1f
                    scaleY = if (isDragging) 1.5f else 1f
                })
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatTime(if (isDragging) (sliderPosition * duration).toLong() else position), color = Color.LightGray, fontSize = 12.sp)
            Text(text = formatTime(duration), color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun PlayerHeader(onBack: () -> Unit, onAddFavorite: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
        Text("ĐANG PHÁT", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Box {
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color.DarkGray)) {
                DropdownMenuItem(
                    text = { Text("Thêm vào yêu thích", color = Color.White) },
                    onClick = {
                        onAddFavorite()
                        showMenu = false
                        Toast.makeText(context, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show()
                    },
                    leadingIcon = { Icon(Icons.Default.Favorite, null, tint = Color.Red) }
                )
            }
        }
    }
}

@Composable
fun SongInfo(title: String, artist: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(text = artist, color = Color(0xFF1DB954), fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PlayerControls(isPlaying: Boolean, onTogglePlay: () -> Unit, onSeekForward: () -> Unit, onSeekBackward: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onSeekBackward) { Icon(Icons.Rounded.Replay10, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
        IconButton(onClick = { }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
        Surface(onClick = onTogglePlay, shape = CircleShape, color = Color.White, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(36.dp)) }
        }
        IconButton(onClick = { }) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
        IconButton(onClick = onSeekForward) { Icon(Icons.Rounded.Forward10, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
