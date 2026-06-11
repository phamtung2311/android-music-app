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
import com.example.zingmp3.network.model.Artist
import com.example.zingmp3.network.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    private val _rawGenres = MutableStateFlow<List<String>>(emptyList())

    val genres: StateFlow<List<String>> = combine(_songs, _rawGenres) { songList, rawGenres ->
        if (rawGenres.isEmpty()) listOf("All")
        else {
            // Đếm số lượng bài hát cho mỗi thể loại
            val genreCounts = songList.groupingBy { it.genre ?: "Unknown" }.eachCount()
            
            // Sắp xếp các thể loại từ server dựa trên số lượng bài hát (nhiều nhất lên đầu)
            val sortedGenres = rawGenres.sortedByDescending { genreName ->
                // Tìm số lượng bài hát của thể loại này (tìm kiếm tương đối vì it.genre có thể chứa nhiều genre)
                songList.count { it.genre?.contains(genreName, ignoreCase = true) == true }
            }
            listOf("All") + sortedGenres
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre

    val songs: StateFlow<List<Song>> = _songs
        .combine(_selectedGenre) { songList, genre ->
            if (genre == "All") songList
            else songList.filter { it.genre?.contains(genre, ignoreCase = true) == true }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setGenre(genre: String) {
        _selectedGenre.value = genre
    }

    private val _topWeeklySongs = MutableStateFlow<List<Song>>(emptyList())
    val topWeeklySongs: StateFlow<List<Song>> = _topWeeklySongs

    val top10WeeklySongs: StateFlow<List<Song>> = _topWeeklySongs
        .map { it.take(10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _username = MutableStateFlow("User")
    val username: StateFlow<String> = _username

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists

    private val _artistSongs = MutableStateFlow<List<Song>>(emptyList())
    val artistSongs: StateFlow<List<Song>> = _artistSongs

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
    private var currentPlayQueue: List<Song> = emptyList()
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
        fetchGenres()
        fetchArtists()
    }

    private fun fetchArtists() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getArtists()
                if (response.isSuccessful) {
                    val artistList = response.body() ?: emptyList()
                    // Sắp xếp nghệ sĩ phổ biến (nhiều người quan tâm nhất lên đầu)
                    _artists.value = artistList.sortedByDescending { it.followers_count }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error fetching artists", e)
            }
        }
    }

    fun fetchArtistSongs(artistId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getArtistSongs(artistId)
                if (response.isSuccessful) {
                    _artistSongs.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error fetching artist songs", e)
            }
        }
    }

    private fun fetchGenres() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getGenres()
                if (response.isSuccessful) {
                    val genreList = response.body()?.map { it.name } ?: emptyList()
                    _rawGenres.value = genreList
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error fetching genres", e)
            }
        }
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

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        val index = currentMediaItemIndex
                        if (index >= 0 && index < currentPlayQueue.size) {
                            val song = currentPlayQueue[index]
                            _currentSong.value = song
                            
                            val userId = getUserId()
                            if (userId != -1) {
                                checkLikeStatus(song.id, userId)
                            }
                            
                            statsJob?.cancel()
                            statsJob = viewModelScope.launch {
                                delay(2000)
                                recordView(song.id)
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                        _isPlaying.value = isPlayingNow
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.e("MusicViewModel", "Player error: ${error.message}", error)
                        _isPlaying.value = false
                    }
                })
            }
    }

    private fun updateProgress() {
        viewModelScope.launch {
            while (true) {
                try {
                    exoPlayer?.let { player ->
                        if (player.playbackState != Player.STATE_IDLE && 
                            player.playbackState != Player.STATE_ENDED) {
                            if (player.isPlaying) {
                                _currentPosition.value = player.currentPosition
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MusicViewModel", "Error in updateProgress", e)
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
        currentPlayQueue = listOf(song)
        _currentSong.value = song
        
        // Reset trạng thái cũ và kiểm tra ngay lập tức
        val userId = getUserId()
        if (userId != -1) {
            checkLikeStatus(song.id, userId)
        } else {
            _isCurrentLiked.value = false
        }
        
        // Hủy job thống kê cũ
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            delay(2000) // Sau khi nghe được 2s mới tính là 1 view
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

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        currentPlayQueue = songs
        
        val firstSong = songs[startIndex]
        _currentSong.value = firstSong
        
        val userId = getUserId()
        if (userId != -1) {
            checkLikeStatus(firstSong.id, userId)
        }
        
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            delay(2000)
            recordView(firstSong.id)
        }

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItems = songs.map { MediaItem.fromUri(it.getFullAudioUrl()) }
            player.addMediaItems(mediaItems)
            player.seekTo(startIndex, 0L)
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
                    // Backend trả về { isFavorite: true/false }
                    val isFav = body?.get("isFavorite") as? Boolean ?: false
                    _isCurrentLiked.value = isFav
                }
            } catch (e: Exception) {
                _isCurrentLiked.value = false
            }
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
