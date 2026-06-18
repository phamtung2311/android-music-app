package com.example.zingmp3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.zingmp3.network.model.Song
import com.example.zingmp3.ui.viewmodel.MusicViewModel
import com.example.zingmp3.ui.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    musicViewModel: MusicViewModel,
    playlistViewModel: PlaylistViewModel
) {
    val searchQuery by musicViewModel.searchQuery.collectAsState()
    val searchResultsSongs by musicViewModel.searchResultsSongs.collectAsState()
    val searchResultsArtists by musicViewModel.searchResultsArtists.collectAsState()
    val searchHistory by musicViewModel.searchHistory.collectAsState()
    
    val focusManager = LocalFocusManager.current
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Tìm kiếm",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { musicViewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Bạn muốn nghe gì?", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { 
                        musicViewModel.setSearchQuery("") 
                    }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (searchQuery.isNotBlank()) {
                    musicViewModel.addToSearchHistory(searchQuery)
                    focusManager.clearFocus()
                }
            }),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF282828),
                unfocusedContainerColor = Color(0xFF282828),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF1DB954),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (searchQuery.isEmpty()) {
                if (searchHistory.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tìm kiếm gần đây",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Xóa tất cả",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable { musicViewModel.clearSearchHistory() }
                            )
                        }
                    }
                    items(searchHistory) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable { 
                                    musicViewModel.setSearchQuery(item)
                                    musicViewModel.addToSearchHistory(item)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = item, color = Color.White, modifier = Modifier.weight(1f))
                            IconButton(onClick = { musicViewModel.removeFromSearchHistory(item) }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Tìm kiếm bài hát hoặc nghệ sĩ yêu thích của bạn",
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 32.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                if (searchResultsArtists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Nghệ sĩ",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(searchResultsArtists, key = { it.id }) { artist ->
                                ArtistItem(artist) {
                                    musicViewModel.addToSearchHistory(searchQuery)
                                    navController.navigate("artist_detail/${artist.id}")
                                }
                            }
                        }
                    }
                }

                if (searchResultsSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Bài hát",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(searchResultsSongs, key = { "search_song_${it.id}" }) { song ->
                        SongListItem(
                            song = song,
                            onClick = {
                                musicViewModel.addToSearchHistory(searchQuery)
                                musicViewModel.playSong(song)
                                navController.navigate("player")
                            },
                            onMoreClick = { songToAddToPlaylist = song },
                            onArtistClick = { artistId ->
                                musicViewModel.addToSearchHistory(searchQuery)
                                navController.navigate("artist_detail/$artistId")
                            }
                        )
                    }
                }

                if (searchResultsSongs.isEmpty() && searchResultsArtists.isEmpty()) {
                    item {
                        Text(
                            text = "Không tìm thấy kết quả cho \"$searchQuery\"",
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 32.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
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
