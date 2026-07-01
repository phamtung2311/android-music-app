package com.example.zingmp3.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.zingmp3.network.model.Song
import com.example.zingmp3.ui.viewmodel.MusicViewModel
import com.example.zingmp3.ui.viewmodel.PlaylistViewModel

@Composable
fun PlayerScreen(
    navController: NavController, 
    musicViewModel: MusicViewModel,
    playlistViewModel: PlaylistViewModel
) {
    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val isShuffleEnabled by musicViewModel.isShuffleEnabled.collectAsState()
    val repeatMode by musicViewModel.repeatMode.collectAsState()

    var showAddToPlaylist by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val backgroundBrush = remember {
        Brush.verticalGradient(listOf(Color(0xFF333333), Color.Black))
    }

    currentSong?.let { song ->
        Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlayerHeader(
                    song = song,
                    musicViewModel = musicViewModel,
                    onBack = { navController.popBackStack() },
                    onAddFavorite = { musicViewModel.favoriteSong(song.id) },
                    onAddToPlaylist = { showAddToPlaylist = true },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ bài hát")
                            putExtra(Intent.EXTRA_TEXT, "Nghe bài hát ${song.title} của ${song.artist_name} trên Muzic: ${song.getFullAudioUrl()}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"))
                    }
                )

                Spacer(modifier = Modifier.weight(0.5f))

                RotatingCD(imageUrl = song.getFullImageUrl(), isPlaying = isPlaying)

                Spacer(modifier = Modifier.weight(0.5f))

                SongStatsRow(song.views, song.likes_count)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = song.title ?: "Không rõ tiêu đề", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            text = song.artist_name ?: "Không rõ nghệ sĩ", 
                            color = Color(0xFF1DB954), 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                song.artist_id?.let { id ->
                                    navController.navigate("artist_detail/$id")
                                }
                            }
                        )
                    }
                    LikeButton(musicViewModel, song.id)
                }

                Spacer(modifier = Modifier.height(24.dp))

                PlaybackProgress(musicViewModel)

                Spacer(modifier = Modifier.height(24.dp))

                PlayerControls(
                    isPlaying = isPlaying,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    onTogglePlay = { 
                        musicViewModel.togglePlayPause()
                        val message = if (isPlaying) "Đã tạm dừng" else "Đã phát nhạc"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    onSkipNext = { 
                        musicViewModel.skipToNext() 
                        Toast.makeText(context, "Phát bài tiếp theo", Toast.LENGTH_SHORT).show()
                    },
                    onSkipPrevious = { 
                        musicViewModel.skipToPrevious() 
                        Toast.makeText(context, "Phát bài trước đó", Toast.LENGTH_SHORT).show()
                    },
                    onToggleShuffle = { 
                        musicViewModel.toggleShuffle()
                        val message = if (!isShuffleEnabled) "Đã bật phát ngẫu nhiên" else "Đã tắt phát ngẫu nhiên"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    onToggleRepeat = { 
                        musicViewModel.toggleRepeatMode()
                        val nextMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> "Lặp lại danh sách"
                            Player.REPEAT_MODE_ALL -> "Lặp lại bài hát này"
                            else -> "Đã tắt lặp lại"
                        }
                        Toast.makeText(context, nextMode, Toast.LENGTH_SHORT).show()
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showAddToPlaylist) {
            AddToPlaylistBottomSheet(
                song = song,
                viewModel = playlistViewModel,
                onDismiss = { showAddToPlaylist = false }
            )
        }
    }
}

@Composable
fun LyricsView(lyrics: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = lyrics,
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LikeButton(viewModel: MusicViewModel, songId: Int) {
    val isLiked by viewModel.isCurrentLiked.collectAsState()
    val context = LocalContext.current
    
    IconButton(
        onClick = { 
            viewModel.likeSong(songId) { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = null,
            tint = if (isLiked) Color.Red else Color.White,
            modifier = Modifier.size(32.dp)
        )
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
fun PlayerHeader(
    song: Song,
    musicViewModel: MusicViewModel,
    onBack: () -> Unit, 
    onAddFavorite: () -> Unit, 
    onAddToPlaylist: () -> Unit, 
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val downloadedSongs by musicViewModel.downloadedSongs.collectAsState()
    val isDownloaded = downloadedSongs.contains(song.id)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
        Text("ĐANG PHÁT", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Box {
            IconButton(onClick = { 
                showMenu = true 
                Toast.makeText(context, "Mở tùy chọn bài hát", Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
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
                DropdownMenuItem(
                    text = { Text("Thêm vào playlist", color = Color.White) },
                    onClick = {
                        onAddToPlaylist()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text(if (isDownloaded) "Đã tải xuống" else "Tải xuống", color = Color.White) },
                    onClick = {
                        if (!isDownloaded) {
                            musicViewModel.downloadSong(song) { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        showMenu = false
                    },
                    leadingIcon = { Icon(if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download, null, tint = if (isDownloaded) Color(0xFF1DB954) else Color.White) }
                )
                DropdownMenuItem(
                    text = { Text("Chia sẻ", color = Color.White) },
                    onClick = {
                        onShare()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Share, null, tint = Color.White) }
                )
            }
        }
    }
}

@Composable
fun PlayerControls(
    isPlaying: Boolean, 
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    onTogglePlay: () -> Unit, 
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                imageVector = Icons.Default.Shuffle, 
                contentDescription = "Shuffle", 
                tint = if (isShuffleEnabled) Color(0xFF1DB954) else Color.White
            )
        }
        
        IconButton(onClick = onSkipPrevious) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
        
        Surface(onClick = onTogglePlay, shape = CircleShape, color = Color.White, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(36.dp)) }
        }
        
        IconButton(onClick = onSkipNext) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
        
        IconButton(onClick = onToggleRepeat) {
            Icon(
                imageVector = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                    Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = if (repeatMode != Player.REPEAT_MODE_OFF) Color(0xFF1DB954) else Color.White
            )
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
