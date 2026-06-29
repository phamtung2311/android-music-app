package com.example.zingmp3.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.zingmp3.network.model.Playlist
import com.example.zingmp3.network.model.Song
import com.example.zingmp3.ui.viewmodel.MusicViewModel
import com.example.zingmp3.ui.viewmodel.PlaylistViewModel

@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: PlaylistViewModel,
    musicViewModel: MusicViewModel
) {
    val playlists by viewModel.playlists.collectAsState()
    val downloadedSongs by musicViewModel.downloadedSongsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Playlists", "Đã tải xuống")

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToEdit by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var playlistIdToPlayAll by remember { mutableStateOf<Int?>(null) }

    val playlistDetail by viewModel.playlistDetail.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchPlaylists()
    }

    LaunchedEffect(playlistDetail) {
        playlistDetail?.let { detail ->
            if (detail.id == playlistIdToPlayAll) {
                if (detail.songs.isNotEmpty()) {
                    musicViewModel.playPlaylist(detail.songs)
                    Toast.makeText(context, "Đang phát tất cả bài hát từ ${detail.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Playlist này chưa có bài hát nào", Toast.LENGTH_SHORT).show()
                }
                playlistIdToPlayAll = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "Thư viện",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF1DB954),
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF1DB954)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, color = if (selectedTab == index) Color.White else Color.Gray) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            PlaylistSection(
                playlists = playlists,
                isLoading = isLoading,
                error = error,
                onCreateClick = { showCreateDialog = true },
                onPlaylistClick = { navController.navigate("playlist_detail/${it.id}") },
                onPlayAll = {
                    playlistIdToPlayAll = it.id
                    viewModel.fetchPlaylistDetails(it.id)
                },
                onEdit = { playlistToEdit = it },
                onDelete = { playlistToDelete = it }
            )
        } else {
            DownloadedSection(
                songs = downloadedSongs,
                onSongClick = { index, song ->
                    musicViewModel.playPlaylist(downloadedSongs, index)
                    navController.navigate("player")
                }
            )
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name) {
                    showCreateDialog = false
                    Toast.makeText(context, "Đã tạo playlist $name", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    playlistToEdit?.let { playlist ->
        EditPlaylistDialog(
            currentName = playlist.name,
            onDismiss = { playlistToEdit = null },
            onUpdate = { newName ->
                viewModel.updatePlaylist(playlist.id, newName) {
                    playlistToEdit = null
                    Toast.makeText(context, "Đã cập nhật tên playlist", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Xóa Playlist") },
            text = { Text("Bạn có chắc chắn muốn xóa playlist '${playlist.name}' không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id) {
                            playlistToDelete = null
                            Toast.makeText(context, "Đã xóa playlist", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun PlaylistSection(
    playlists: List<Playlist>,
    isLoading: Boolean,
    error: String?,
    onCreateClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onPlayAll: (Playlist) -> Unit,
    onEdit: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit
) {
    Column {
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tạo Playlist mới", color = Color.White)
        }

        if (isLoading && playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1DB954))
            }
        } else if (error != null && playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error, color = Color.Red)
            }
        } else if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Bạn chưa có playlist nào", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(playlists) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist) },
                        onPlayAll = { onPlayAll(playlist) },
                        onEdit = { onEdit(playlist) },
                        onDelete = { onDelete(playlist) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadedSection(songs: List<Song>, onSongClick: (Int, Song) -> Unit) {
    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Chưa có nhạc tải xuống", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Nhạc bạn tải xuống sẽ xuất hiện tại đây.", color = Color.Gray, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(songs) { index: Int, song: Song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongClick(index, song) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.getFullImageUrl(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop,
                        error = rememberVectorPainter(Icons.Default.MusicNote)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = song.title ?: "Unknown", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = song.artist_name ?: "Unknown", color = Color.Gray, fontSize = 14.sp)
                    }
                    Icon(Icons.Default.DownloadDone, contentDescription = null, tint = Color(0xFF1DB954))
                }
            }
        }
    }
}

@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlayAll: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.getFullImageUrl(),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            error = rememberVectorPainter(Icons.Default.MusicNote)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = playlist.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "${playlist.realSongsCount} bài hát", color = Color.Gray, fontSize = 14.sp)
        }
        var showMenu by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color.DarkGray)
            ) {
                DropdownMenuItem(
                    text = { Text("Phát tất cả", color = Color.White) },
                    onClick = { onPlayAll(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text("Sửa tên", color = Color.White) },
                    onClick = { onEdit(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text("Xóa", color = Color.White) },
                    onClick = { onDelete(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistDialog(currentName: String, onDismiss: () -> Unit, onUpdate: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sửa tên Playlist") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên mới") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onUpdate(name) }) { Text("Cập nhật") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo Playlist mới") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên playlist") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(name) }) { Text("Tạo") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
