package com.example.zingmp3.ui.viewmodel

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.example.zingmp3.network.RetrofitClient
import com.example.zingmp3.player.PlayerManager
import com.example.zingmp3.service.PlaybackService
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
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

    private val sharedPref = application.getSharedPreferences("USER_DATA", android.content.Context.MODE_PRIVATE)

    // --- Private StateFlows ---
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    private val _rawGenres = MutableStateFlow<List<String>>(emptyList())
    private val _selectedGenre = MutableStateFlow("All")
    private val _topWeeklySongs = MutableStateFlow<List<Song>>(emptyList())
    private val _username = MutableStateFlow("User")
    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    private val _artistSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _currentSong = MutableStateFlow<Song?>(null)
    private val _isPlaying = MutableStateFlow(false)
    private val _currentPosition = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)
    private val _isBuffering = MutableStateFlow(false)
    private val _isCurrentLiked = MutableStateFlow(false)
    private val _isArtistFollowed = MutableStateFlow(false)
    private val _followedArtistIds = MutableStateFlow<Set<Int>>(loadFollowedArtistIds())
    private val _historyIds = MutableStateFlow<List<Int>>(loadHistory())
    private val _searchQuery = MutableStateFlow("")
    private val _searchHistory = MutableStateFlow<List<String>>(loadSearchHistory())

    // --- Public StateFlows ---
    val genres: StateFlow<List<String>> = combine(_songs, _rawGenres) { songList, rawGenres ->
        if (rawGenres.isEmpty()) listOf("All")
        else {
            val sortedGenres = rawGenres.sortedByDescending { genreName ->
                songList.count { it.genre?.contains(genreName, ignoreCase = true) == true }
            }
            listOf("All") + sortedGenres
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    val selectedGenre: StateFlow<String> = _selectedGenre

    val songs: StateFlow<List<Song>> = _songs
        .combine(_selectedGenre) { songList, genre ->
            if (genre == "All") songList
            else songList
                .filter { it.genre?.contains(genre, ignoreCase = true) == true }
                .sortedByDescending { it.views }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topWeeklySongs: StateFlow<List<Song>> = _topWeeklySongs
    val top20WeeklySongs: StateFlow<List<Song>> = _topWeeklySongs
        .map { it.take(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val username: StateFlow<String> = _username
    val artists: StateFlow<List<Artist>> = _artists
    val artistSongs: StateFlow<List<Song>> = _artistSongs
    val currentSong: StateFlow<Song?> = _currentSong
    val isPlaying: StateFlow<Boolean> = _isPlaying
    val currentPosition: StateFlow<Long> = _currentPosition
    val duration: StateFlow<Long> = _duration
    val isBuffering: StateFlow<Boolean> = _isBuffering
    val isCurrentLiked: StateFlow<Boolean> = _isCurrentLiked
    val isArtistFollowed: StateFlow<Boolean> = _isArtistFollowed
    val searchQuery: StateFlow<String> = _searchQuery
    val searchHistory: StateFlow<List<String>> = _searchHistory

    val searchResultsSongs: StateFlow<List<Song>> = combine(_songs, _searchQuery) { songList, query ->
        if (query.isBlank()) emptyList()
        else songList.filter { 
            it.title?.contains(query, ignoreCase = true) == true || 
            it.artist_name?.contains(query, ignoreCase = true) == true 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResultsArtists: StateFlow<List<Artist>> = combine(_artists, _searchQuery) { artistList, query ->
        if (query.isBlank()) emptyList()
        else artistList.filter { it.stage_name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val followedArtists: StateFlow<List<Artist>> = combine(_artists, _followedArtistIds) { allArtists, followedIds ->
        allArtists.filter { it.id in followedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recommendation logic
    val recommendedSongs: StateFlow<List<Song>> = combine(_songs, _historyIds) { allSongs, historyIds ->
        if (allSongs.isEmpty()) return@combine emptyList()
        if (historyIds.isEmpty()) return@combine allSongs.shuffled().take(20)

        val historySongs = historyIds.mapNotNull { id -> allSongs.find { it.id == id } }
        val recent5 = historySongs.take(5)
        
        val historyGenres = historySongs.flatMap { it.genre?.split(",")?.map { g -> g.trim() } ?: emptyList() }.toSet()
        val historyArtistIds = historySongs.mapNotNull { it.artist_id }.toSet()

        val relatedByArtist = allSongs.filter { it.artist_id in historyArtistIds && it.id !in historyIds }
        val relatedByGenre = allSongs.filter { song -> 
            val songGenres = song.genre?.split(",")?.map { it.trim() } ?: emptyList()
            songGenres.any { it in historyGenres } && song.id !in historyIds 
        }

        val combined = (recent5 + (relatedByArtist + relatedByGenre).distinct().shuffled()).take(20)
        
        if (combined.size < 10 && allSongs.size > combined.size) {
            (combined + (allSongs - combined.toSet()).shuffled()).take(20)
        } else {
            combined
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendedArtists: StateFlow<List<Artist>> = _artists
        .map { it.sortedByDescending { artist -> artist.followers_count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var exoPlayer: ExoPlayer? = null
    private var isPlayerListenerAdded = false
    // currentPlayQueue is now in PlayerManager
    private var statsJob: Job? = null

    init {
        setupPlayer()
        refreshData()
        updateProgress()
        loadUsername()
        // Khởi động service ngay để chuẩn bị MediaSession
        val intent = Intent(getApplication(), PlaybackService::class.java)
        getApplication<Application>().startService(intent)
    }

    private fun loadUsername() {
        _username.value = sharedPref.getString("username", "User") ?: "User"
    }

    private fun loadHistory(): List<Int> {
        val historyStr = sharedPref.getString("listening_history", "") ?: ""
        return if (historyStr.isEmpty()) emptyList() else historyStr.split(",").mapNotNull { it.toIntOrNull() }
    }

    private fun loadFollowedArtistIds(): Set<Int> {
        val idsStr = sharedPref.getString("followed_artist_ids", "") ?: ""
        return if (idsStr.isEmpty()) emptySet() else idsStr.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    private fun loadSearchHistory(): List<String> {
        val historyStr = sharedPref.getString("search_history", "") ?: ""
        return if (historyStr.isEmpty()) emptyList() else historyStr.split("|")
    }

    private fun saveSearchHistory(history: List<String>) {
        sharedPref.edit().putString("search_history", history.joinToString("|")).apply()
    }

    private fun saveFollowedArtistIds(ids: Set<Int>) {
        sharedPref.edit().putString("followed_artist_ids", ids.joinToString(",")).apply()
    }

    private fun saveHistory(ids: List<Int>) {
        sharedPref.edit().putString("listening_history", ids.joinToString(",")).apply()
    }

    private fun addToHistory(songId: Int) {
        val currentHistory = _historyIds.value.toMutableList()
        currentHistory.remove(songId)
        currentHistory.add(0, songId)
        if (currentHistory.size > 50) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        _historyIds.value = currentHistory
        saveHistory(currentHistory)
    }

    fun setGenre(genre: String) {
        _selectedGenre.value = genre
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToSearchHistory(query: String) {
        if (query.isBlank()) return
        val currentHistory = _searchHistory.value.toMutableList()
        currentHistory.remove(query)
        currentHistory.add(0, query)
        val newHistory = currentHistory.take(8)
        _searchHistory.value = newHistory
        saveSearchHistory(newHistory)
    }

    fun removeFromSearchHistory(query: String) {
        val newHistory = _searchHistory.value.filter { it != query }
        _searchHistory.value = newHistory
        saveSearchHistory(newHistory)
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        saveSearchHistory(emptyList())
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
                    _artists.value = artistList.sortedByDescending { it.followers_count }
                    
                    // Tự động đồng bộ trạng thái quan tâm cho những nghệ sĩ mới tải về
                    syncFollowedStatus(artistList)
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error fetching artists", e)
            }
        }
    }

    private fun syncFollowedStatus(artistList: List<Artist>) {
        val userId = getUserId()
        if (userId == -1) return
        
        viewModelScope.launch {
            val currentIds = _followedArtistIds.value.toMutableSet()
            var changed = false
            
            artistList.forEach { artist ->
                try {
                    val response = RetrofitClient.api.checkFollowStatus(artist.id, userId)
                    if (response.isSuccessful) {
                        val isFollowing = response.body()?.get("isFollowing") as? Boolean ?: false
                        if (isFollowing) {
                            if (currentIds.add(artist.id)) changed = true
                        } else {
                            if (currentIds.remove(artist.id)) changed = true
                        }
                    }
                } catch (e: Exception) { }
            }
            
            if (changed) {
                _followedArtistIds.value = currentIds
                saveFollowedArtistIds(currentIds)
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
        val player = PlayerManager.getPlayer(getApplication())
        
        if (!isPlayerListenerAdded) {
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    _isBuffering.value = (state == Player.STATE_BUFFERING)
                    if (state == Player.STATE_READY) {
                        _duration.value = player.duration
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    val index = player.currentMediaItemIndex
                    val queue = PlayerManager.currentPlayQueue
                    if (index >= 0 && index < queue.size) {
                        val song = queue[index]
                        _currentSong.value = song
                        addToHistory(song.id)
                        
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
                    if (isPlayingNow) {
                        ensureServiceRunning()
                    }
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("MusicViewModel", "Player error: ${error.message}", error)
                    _isPlaying.value = false
                }
            })
            isPlayerListenerAdded = true
        }
        
        exoPlayer = player
        // mediaSession sẽ được khởi tạo bởi PlaybackService
    }

    private fun ensureServiceRunning() {
        val intent = Intent(getApplication(), PlaybackService::class.java)
        // Dùng startForegroundService để báo cho Android biết đây là dịch vụ quan trọng (phát nhạc)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
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

    private fun createMediaItem(song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist_name)
            .setArtworkUri(android.net.Uri.parse(song.getFullImageUrl()))
            .build()
        
        return MediaItem.Builder()
            .setUri(song.getFullAudioUrl())
            .setMediaId(song.id.toString())
            .setMediaMetadata(metadata)
            .build()
    }

    fun playSong(song: Song) {
        PlayerManager.currentPlayQueue = listOf(song)
        _currentSong.value = song
        addToHistory(song.id)
        
        val userId = getUserId()
        if (userId != -1) {
            checkLikeStatus(song.id, userId)
        } else {
            _isCurrentLiked.value = false
        }
        
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            delay(2000) 
            recordView(song.id)
        }

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = createMediaItem(song)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            ensureServiceRunning()
        }
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        PlayerManager.currentPlayQueue = songs
        
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
            val mediaItems = songs.map { createMediaItem(it) }
            player.addMediaItems(mediaItems)
            player.seekTo(startIndex, 0L)
            player.prepare()
            player.play()
            ensureServiceRunning()
        }
    }

    fun checkFollowStatus(artistId: Int) {
        val userId = getUserId()
        if (userId == -1) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.checkFollowStatus(artistId, userId)
                if (response.isSuccessful) {
                    val body = response.body()
                    val isFollowing = body?.get("isFollowing") as? Boolean ?: false
                    _isArtistFollowed.value = isFollowing
                    
                    // Sync local list
                    val currentIds = _followedArtistIds.value.toMutableSet()
                    if (isFollowing) currentIds.add(artistId) else currentIds.remove(artistId)
                    if (currentIds != _followedArtistIds.value) {
                        _followedArtistIds.value = currentIds
                        saveFollowedArtistIds(currentIds)
                    }
                }
            } catch (e: Exception) {
                _isArtistFollowed.value = false
            }
        }
    }

    fun followArtist(artistId: Int) {
        val userId = getUserId()
        if (userId == -1) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.followArtist(artistId, userId)
                if (response.isSuccessful) {
                    val body = response.body()
                    val isFollowing = body?.get("isFollowing") as? Boolean ?: false
                    _isArtistFollowed.value = isFollowing
                    
                    // Update local list
                    val currentIds = _followedArtistIds.value.toMutableSet()
                    if (isFollowing) currentIds.add(artistId) else currentIds.remove(artistId)
                    _followedArtistIds.value = currentIds
                    saveFollowedArtistIds(currentIds)
                    
                    fetchArtists() 
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    private fun checkLikeStatus(songId: Int, userId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.checkFavoriteStatus(songId, userId)
                if (response.isSuccessful) {
                    val body = response.body()
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
                    fetchSongs()
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

    fun stopAndClear() {
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
        }
        _currentSong.value = null
        _isPlaying.value = false
    }

    override fun onCleared() {
        super.onCleared()
        // Không giải phóng ở đây để nhạc có thể tiếp tục phát ở nền (background)
        // Giải phóng sẽ được thực hiện khi Service bị đóng hoặc thông qua PlayerManager
    }
}
