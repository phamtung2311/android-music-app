package com.example.zingmp3.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.zingmp3.network.model.Song
import com.example.zingmp3.ui.viewmodel.MusicViewModel
import com.example.zingmp3.ui.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    musicViewModel: MusicViewModel,
    playlistViewModel: PlaylistViewModel,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val context = LocalContext.current
    val downloadedSongs by musicViewModel.downloadedSongs.collectAsState()
    val isDownloaded = downloadedSongs.contains(song.id)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF282828),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Song Info Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.getFullImageUrl(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = song.title ?: "Unknown", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = song.artist_name ?: "Unknown", color = Color.Gray, fontSize = 14.sp)
                }
            }

            HorizontalDivider(color = Color.DarkGray)

            // Options
            OptionItem(
                icon = Icons.Default.PlaylistAdd,
                text = "Thêm vào playlist",
                onClick = {
                    onAddToPlaylist()
                }
            )

            OptionItem(
                icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                text = if (isDownloaded) "Đã tải xuống" else "Tải xuống",
                iconColor = if (isDownloaded) Color(0xFF1DB954) else Color.White,
                onClick = {
                    if (!isDownloaded) {
                        musicViewModel.downloadSong(song) { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    onDismiss()
                }
            )

            OptionItem(
                icon = Icons.Default.Share,
                text = "Chia sẻ",
                onClick = {
                    // Reuse share logic or placeholder
                    Toast.makeText(context, "Tính năng chia sẻ", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )
            
            OptionItem(
                icon = Icons.Default.Info,
                text = "Xem thông tin bài hát",
                onClick = { onDismiss() }
            )
        }
    }
}

@Composable
fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = text, color = Color.White, fontSize = 16.sp)
    }
}
