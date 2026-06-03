package com.example.zingmp3.ui.viewmodel

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.example.zingmp3.network.RetrofitClient
import com.example.zingmp3.network.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _topWeeklySongs = MutableStateFlow<List<Song>>(emptyList())
    val topWeeklySongs: StateFlow<List<Song>> = _topWeeklySongs

    val top10WeeklySongs: StateFlow<List<Song>> = _topWeeklySongs
        .map { it.take(10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _username = MutableStateFlow("User")
    val username: StateFlow<String> = _username

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    private val _isCurrentLiked = MutableStateFlow(false)
    val isCurrentLiked: StateFlow<Boolean> = _isCurrentLiked

    private var exoPlayer: ExoPlayer? = null
    private val sharedPref = application.getSharedPreferences("USER_DATA", android.content.Context.MODE_PRIVATE)
    private var statsJob: Job? = null

    init {
        setupPlayer()
        refreshData()
        updateProgress()
        loadUsername()
    }

    private fun loadUsername() {
        _username.value = sharedPref.getString("username", "User") ?: "User"
    }

    fun refreshData() {
        fetchSongs()
        fetchTopWeekly()
    }

    private fun getUserId(): Int = sharedPref.getInt("userId", -1)

    @OptIn(UnstableApi::class)
    private fun setupPlayer() {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(50000, 100000, 1500, 3000)
            .build()

        exoPlayer = ExoPlayer.Builder(getApplication())
            .setLoadControl(loadControl)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        _isBuffering.value = (state == Player.STATE_BUFFERING)
                        if (state == Player.STATE_READY) {
                            _duration.value = duration
                        }
                    }
                    override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                        _isPlaying.value = isPlayingNow
                    }
                })
            }
    }

    private fun updateProgress() {
        viewModelScope.launch {
            while (true) {
                exoPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition
                    }
                }
                delay(1000)
            }
        }
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    private fun fetchSongs() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getSongs()
                if (response.isSuccessful) {
                    val songList = response.body() ?: emptyList()
                    _songs.value = songList
                    _currentSong.value?.let { current ->
                        songList.find { it.id == current.id }?.let { updated ->
                            _currentSong.value = updated
                        }
                    }
                } else {
                    android.util.Log.e("MusicViewModel", "Fetch songs failed: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error fetching songs", e)
            }
        }
    }

    fun fetchTopWeekly() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getTopWeeklySongs()
                if (response.isSuccessful) {
                    _topWeeklySongs.value = response.body() ?: emptyList()
                } else {
                    android.util.Log.e("MusicViewModel", "Fetch top weekly failed: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error fetching top weekly", e)
            }
        }
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _isCurrentLiked.value = false
        
        // Hủy job cũ nếu có
        statsJob?.cancel()
        
        // Trì hoãn các API mạng để ưu tiên băng thông cho việc tải nhạc mượt mà
        statsJob = viewModelScope.launch {
            delay(1500) // Đợi 1.5 giây để nhạc load mượt rồi mới check status
            val userId = getUserId()
            if (userId != -1) {
                checkLikeStatus(song.id, userId)
            }
            recordView(song.id)
        }

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.fromUri(song.getFullAudioUrl())
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    private fun checkLikeStatus(songId: Int, userId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.checkFavoriteStatus(songId, userId)
                if (response.isSuccessful) {
                    val body = response.body()
                    val isFav = body?.get("isFavorite") as? Boolean 
                                ?: body?.get("isLiked") as? Boolean 
                                ?: false
                    _isCurrentLiked.value = isFav
                }
            } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun recordView(songId: Int) {
        val userId = getUserId()
        viewModelScope.launch {
            try {
                RetrofitClient.api.recordView(songId, if (userId != -1) userId else null)
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun likeSong(songId: Int, onError: (String) -> Unit) {
        val userId = getUserId()
        if (userId == -1) {
            onError("Vui lòng đăng nhập")
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.likeSong(songId, userId)
                if (response.isSuccessful) {
                    val body = response.body()
                    val isLikedNow = body?.get("isLiked") as? Boolean 
                                    ?: body?.get("liked") as? Boolean 
                                    ?: !_isCurrentLiked.value
                    
                    _isCurrentLiked.value = isLikedNow
                    fetchSongs() // Cập nhật lại số lượng Like hiển thị
                } else {
                    onError("Lỗi máy chủ (${response.code()})")
                }
            } catch (e: Exception) {
                onError("Lỗi kết nối mạng")
            }
        }
    }

    fun favoriteSong(songId: Int) {
        val userId = getUserId()
        if (userId == -1) return
        viewModelScope.launch {
            try {
                RetrofitClient.api.favoriteSong(songId, userId)
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
