package com.example.zingmp3.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zingmp3.data.repository.PlaylistRepository
import com.example.zingmp3.network.model.Playlist
import com.example.zingmp3.network.model.PlaylistDetail
import com.example.zingmp3.network.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaylistRepository()
    private val sharedPref = application.getSharedPreferences("USER_DATA", Context.MODE_PRIVATE)

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _playlistDetail = MutableStateFlow<PlaylistDetail?>(null)
    val playlistDetail: StateFlow<PlaylistDetail?> = _playlistDetail

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private fun getUserId(): Int = sharedPref.getInt("userId", -1)

    fun fetchPlaylists() {
        val userId = getUserId()
        if (userId == -1) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = repository.getPlaylists(userId)
                if (response.isSuccessful) {
                    _playlists.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to load playlists"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPlaylist(name: String, onSuccess: () -> Unit) {
        val userId = getUserId()
        if (userId == -1) {
            _error.value = "Vui lòng đăng nhập lại"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("PlaylistViewModel", "Creating playlist: $name for user: $userId")
                val response = repository.createPlaylist(name, userId)
                if (response.isSuccessful) {
                    android.util.Log.d("PlaylistViewModel", "Playlist created successfully")
                    fetchPlaylists()
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("PlaylistViewModel", "Failed to create playlist: $errorBody")
                    _error.value = "Lỗi từ server: $errorBody"
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaylistViewModel", "Exception creating playlist", e)
                _error.value = "Lỗi kết nối: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchPlaylistDetails(playlistId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = repository.getPlaylistDetails(playlistId)
                if (response.isSuccessful) {
                    _playlistDetail.value = response.body()
                } else {
                    _error.value = "Failed to load playlist details"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSongToPlaylist(playlistId: Int, songId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.addSongToPlaylist(playlistId, songId)
                if (response.isSuccessful) {
                    fetchPlaylists() // Refresh to update song count
                    onSuccess()
                } else {
                    // Handle error (e.g., song already in playlist)
                    val errorMsg = response.errorBody()?.string() ?: "Failed to add song"
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Int, songId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.removeSongFromPlaylist(playlistId, songId)
                if (response.isSuccessful) {
                    fetchPlaylistDetails(playlistId)
                    fetchPlaylists() // Update counts
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
