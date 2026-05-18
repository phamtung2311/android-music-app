package com.example.zingmp3.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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

    // Tối ưu hóa Gradient: Chỉ tạo 1 lần duy nhất
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
            // Header
            PlayerHeader(onBack = { navController.popBackStack() })

            Spacer(modifier = Modifier.weight(1f))

            // Đĩa CD Quay - Sử dụng kỹ thuật vẽ trực tiếp lên GPU
            RotatingCD(imageUrl = song.getFullImageUrl(), isPlaying = isPlaying)

            Spacer(modifier = Modifier.weight(1f))

            // Thông tin bài hát
            SongInfo(title = song.title, artist = song.artist_name ?: "Unknown Artist")

            Spacer(modifier = Modifier.height(32.dp))

            // Thanh tiến trình - Tách biệt hoàn toàn để không gây lag cho đĩa quay
            PlaybackProgress(musicViewModel)

            Spacer(modifier = Modifier.height(24.dp))

            // Nút điều khiển
            PlayerControls(
                isPlaying = isPlaying,
                onTogglePlay = { musicViewModel.togglePlayPause() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RotatingCD(imageUrl: String?, isPlaying: Boolean) {
    // Sử dụng InfiniteTransition với cấu hình tối ưu
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

    // Sử dụng Painter để kiểm soát việc vẽ tốt hơn AsyncImage trực tiếp
    val painter = rememberAsyncImagePainter(model = imageUrl)

    Box(
        modifier = Modifier
            .size(280.dp)
            .graphicsLayer {
                // KỸ THUẬT QUAN TRỌNG: Chỉ đọc giá trị .value bên trong lambda này.
                // Điều này giúp việc xoay diễn ra ở "Draw Phase", không gây Recomposition.
                rotationZ = if (isPlaying) rotation.value else 0f
            }
            .clip(CircleShape)
            .background(Color.Black)
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Tạo hiệu ứng lỗ tròn nhỏ ở giữa đĩa CD cho chuyên nghiệp
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackProgress(musicViewModel: MusicViewModel) {
    val position by musicViewModel.currentPosition.collectAsState()
    val duration by musicViewModel.duration.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Slider(
            value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
            onValueChange = { musicViewModel.seekTo((it * duration).toLong()) },
            colors = SliderDefaults.colors(
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
            thumb = {
                // Tự định nghĩa chấm tròn trắng nhỏ gọn
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.White, CircleShape)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position),
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTime(duration),
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PlayerHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text("ĐANG PHÁT", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = { }) {
            Icon(Icons.Default.MoreVert, null, tint = Color.White)
        }
    }
}

@Composable
fun SongInfo(title: String, artist: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title, 
            color = Color.White, 
            fontSize = 22.sp, 
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = artist, 
            color = Color(0xFF1DB954), // Đổi màu nghệ sĩ sang xanh cho nổi bật
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PlayerControls(isPlaying: Boolean, onTogglePlay: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }) { Icon(Icons.Default.Shuffle, null, tint = Color.Gray, modifier = Modifier.size(24.dp)) }
        IconButton(onClick = { }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
        
        Surface(
            onClick = onTogglePlay,
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        
        IconButton(onClick = { }) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
        IconButton(onClick = { }) { Icon(Icons.Default.Repeat, null, tint = Color.Gray, modifier = Modifier.size(24.dp)) }
    }
}

private fun formatTime(milliseconds: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
