package com.example.zingmp3.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.zingmp3.network.RetrofitClient
import com.example.zingmp3.network.model.Artist
import com.example.zingmp3.network.model.Song
import com.example.zingmp3.player.PlayerManager
import com.example.zingmp3.service.PlaybackService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences("USER_DATA", android.content.Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- Private StateFlows ---
    private val _songs = MutableStateFlow<List<Song>>(loadCachedSongs())
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
    private val _historyIds = MutableStateFlow<List<Int>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _searchHistory = MutableStateFlow<List<String>>(loadSearchHistory())
    private val _isShuffleEnabled = MutableStateFlow(false)
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    private val _downloadedSongs = MutableStateFlow<Set<Int>>(emptySet())
    private val _downloadedSongsMetadata = MutableStateFlow<List<Song>>(loadDownloadedMetadata())

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
    val songs: StateFlow<List<Song>> = _songs.combine(_selectedGenre) { list, genre ->
        if (genre == "All") list else list.filter { it.genre?.contains(genre, true) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val top20WeeklySongs: StateFlow<List<Song>> = _topWeeklySongs.map { it.take(20) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled
    val repeatMode: StateFlow<Int> = _repeatMode
    val downloadedSongs: StateFlow<Set<Int>> = _downloadedSongs
    val downloadedSongsList: StateFlow<List<Song>> = _downloadedSongsMetadata

    val followedArtists: StateFlow<List<Artist>> = combine(_artists, _followedArtistIds) { allArtists, followedIds ->
        allArtists.filter { followedIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResultsSongs: StateFlow<List<Song>> = combine(_songs, _searchQuery) { list, query ->
        if (query.isBlank()) emptyList() else list.filter { it.title?.contains(query, true) == true || it.artist_name?.contains(query, true) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResultsArtists: StateFlow<List<Artist>> = combine(_artists, _searchQuery) { list, query ->
        if (query.isBlank()) emptyList() else list.filter { it.stage_name.contains(query, true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendedSongs: StateFlow<List<Song>> = _songs.map { it.shuffled().take(10) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recommendedArtists: StateFlow<List<Artist>> = _artists.map { it.sortedByDescending { a -> a.followers_count }.take(10) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var exoPlayer: ExoPlayer? = null
    private var isPlayerListenerAdded = false
    private var statsJob: Job? = null

    init {
        _downloadedSongs.value = _downloadedSongsMetadata.value.map { it.id }.toSet()
        refreshData()
        loadUsername()
        setupPlayer()
        restorePlaybackState()
    }

    // --- Persistence & Metadata Logic ---

    private fun loadDownloadedMetadata(): List<Song> {
        return try {
            val json = sharedPref?.getString("downloaded_metadata", null)
            if (json != null) {
                val type = object : TypeToken<List<Song>>() {}.type
                gson.fromJson(json, type)
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun saveDownloadedMetadata(songs: List<Song>) {
        sharedPref?.edit()?.putString("downloaded_metadata", gson.toJson(songs))?.apply()
    }

    private fun loadCachedSongs(): List<Song> {
        val json = sharedPref?.getString("cached_songs", null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<Song>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) { emptyList() }
        } else emptyList()
    }

    private fun saveCachedSongs(songs: List<Song>) {
        sharedPref?.edit()?.putString("cached_songs", gson.toJson(songs))?.apply()
    }

    private fun loadSearchHistory(): List<String> = try { sharedPref?.getStringSet("search_history", emptySet())?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
    
    private fun saveSearchHistory(history: List<String>) {
        sharedPref?.edit()?.putStringSet("search_history", history.toSet())?.apply()
    }

    private fun loadFollowedArtistIds(): Set<Int> = try { sharedPref?.getStringSet("followed_artists", emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet() } catch (e: Exception) { emptySet() }
    
    private fun saveFollowedArtistIds(ids: Set<Int>) {
        sharedPref?.edit()?.putStringSet("followed_artists", ids.map { it.toString() }.toSet())?.apply()
    }

    // --- Player Core ---

    private fun setupPlayer() {
        exoPlayer = PlayerManager.getPlayer(getApplication())
        exoPlayer?.let { player ->
            if (!isPlayerListenerAdded) {
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        _isBuffering.value = state == Player.STATE_BUFFERING
                        if (state == Player.STATE_READY) _duration.value = player.duration
                        if (state == Player.STATE_ENDED) _isPlaying.value = false
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val currentQueue = PlayerManager.currentPlayQueue
                        val index = player.currentMediaItemIndex
                        if (index in currentQueue.indices) {
                            val song = currentQueue[index]
                            _currentSong.value = song
                            checkLikeStatus(song.id, getUserId())
                            savePlaybackState()
                            recordView(song.id)
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) updateProgress()
                    }
                    
                    override fun onPlayerError(error: PlaybackException) {
                        player.seekToNext()
                        player.prepare()
                        player.play()
                    }
                })
                isPlayerListenerAdded = true
            }
            _isShuffleEnabled.value = player.shuffleModeEnabled
            _repeatMode.value = player.repeatMode
            _isPlaying.value = player.isPlaying
        }
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        PlayerManager.currentPlayQueue = songs
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItems = songs.map { createMediaItem(it) }
            player.setMediaItems(mediaItems)
            player.seekTo(if (startIndex in songs.indices) startIndex else 0, 0L)
            player.prepare()
            player.play()
            ensureServiceRunning()
            savePlaybackState()
        }
    }

    fun playSong(song: Song) {
        if (_currentSong.value?.id == song.id && exoPlayer?.playbackState != Player.STATE_ENDED) {
            exoPlayer?.play()
            return
        }
        playPlaylist(listOf(song), 0)
    }

    // --- Downloads ---

    fun downloadSong(song: Song, onResult: (String) -> Unit) {
        val downloadDir = getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC) ?: return
        val destinationFile = java.io.File(downloadDir, "${song.id}.mp3")
        if (destinationFile.exists()) {
            onResult("Bài hát đã có sẵn")
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().url(song.getFullAudioUrl()).build()).execute()
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { input -> destinationFile.outputStream().use { output -> input.copyTo(output) } }
                    val currentMetadata = _downloadedSongsMetadata.value.toMutableList()
                    if (currentMetadata.none { it.id == song.id }) {
                        currentMetadata.add(song)
                        _downloadedSongsMetadata.value = currentMetadata
                        saveDownloadedMetadata(currentMetadata)
                    }
                    val currentIds = _downloadedSongs.value.toMutableSet()
                    currentIds.add(song.id)
                    _downloadedSongs.value = currentIds
                    launch(kotlinx.coroutines.Dispatchers.Main) { onResult("Tải xuống thành công") }
                }
            } catch (e: Exception) {
                launch(kotlinx.coroutines.Dispatchers.Main) { onResult("Lỗi tải xuống: ${e.message}") }
            }
        }
    }

    private fun createMediaItem(song: Song): MediaItem {
        val downloadDir = getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
        val localFile = java.io.File(downloadDir, "${song.id}.mp3")
        val uri = if (localFile.exists()) android.net.Uri.fromFile(localFile) else android.net.Uri.parse(song.getFullAudioUrl())

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist_name).setArtworkUri(android.net.Uri.parse(song.getFullImageUrl() ?: "")).build())
            .build()
    }

    // --- Others ---
    fun togglePlayPause() { exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun skipToNext() { exoPlayer?.seekToNext() }
    fun skipToPrevious() { if ((exoPlayer?.currentPosition ?: 0) > 5000) exoPlayer?.seekTo(0) else exoPlayer?.seekToPrevious() }
    fun toggleShuffle() { exoPlayer?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled; _isShuffleEnabled.value = it.shuffleModeEnabled } }
    fun toggleRepeatMode() {
        exoPlayer?.let {
            val next = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }
            it.repeatMode = next; _repeatMode.value = next
        }
    }

    fun addToSearchHistory(query: String) {
        val current = _searchHistory.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        val newHistory = current.take(10)
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

    private fun restorePlaybackState() {
        val lastQueueJson = sharedPref?.getString("last_queue", null)
        val lastIndex = sharedPref?.getInt("last_index", -1) ?: -1
        if (lastQueueJson != null && lastIndex != -1) {
            try {
                val queue: List<Song> = gson.fromJson(lastQueueJson, object : TypeToken<List<Song>>() {}.type)
                if (queue.isNotEmpty()) {
                    PlayerManager.currentPlayQueue = queue
                    viewModelScope.launch { delay(1000); exoPlayer?.setMediaItems(queue.map { createMediaItem(it) }); exoPlayer?.seekTo(lastIndex, 0L); exoPlayer?.prepare() }
                }
            } catch (e: Exception) {}
        }
    }

    private fun savePlaybackState() {
        val queue = PlayerManager.currentPlayQueue
        val index = exoPlayer?.currentMediaItemIndex ?: -1
        if (queue.isNotEmpty() && index != -1) sharedPref?.edit()?.putString("last_queue", gson.toJson(queue))?.putInt("last_index", index)?.apply()
    }

    fun loadUsername() { _username.value = sharedPref?.getString("username", "User") ?: "User" }
    fun setGenre(genre: String) { _selectedGenre.value = genre }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun refreshData() { fetchSongs(); fetchTopWeekly(); fetchArtists(); fetchGenres() }
    fun fetchArtists() { viewModelScope.launch { try { val r = RetrofitClient.api.getArtists(); if (r.isSuccessful) _artists.value = r.body() ?: emptyList() } catch (e: Exception) {} } }
    fun fetchArtistSongs(id: Int) { viewModelScope.launch { try { val r = RetrofitClient.api.getArtistSongs(id); if (r.isSuccessful) _artistSongs.value = r.body() ?: emptyList() } catch (e: Exception) {} } }
    fun fetchGenres() { viewModelScope.launch { try { val r = RetrofitClient.api.getGenres(); if (r.isSuccessful) _rawGenres.value = r.body()?.map { it.name } ?: emptyList() } catch (e: Exception) {} } }
    private fun getUserId(): Int = sharedPref?.getInt("userId", -1) ?: -1
    private fun ensureServiceRunning() { val intent = android.content.Intent(getApplication(), PlaybackService::class.java); if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) getApplication<Application>().startForegroundService(intent) else getApplication<Application>().startService(intent) }
    private fun updateProgress() { viewModelScope.launch { while (_isPlaying.value) { _currentPosition.value = exoPlayer?.currentPosition ?: 0L; delay(1000) } } }
    fun seekTo(pos: Long) { exoPlayer?.seekTo(pos) }
    fun fetchSongs() { viewModelScope.launch { try { val r = RetrofitClient.api.getSongs(); if (r.isSuccessful) { val list = r.body() ?: emptyList(); _songs.value = list; saveCachedSongs(list) } } catch (e: Exception) {} } }
    fun fetchTopWeekly() { viewModelScope.launch { try { val r = RetrofitClient.api.getTopWeeklySongs(); if (r.isSuccessful) _topWeeklySongs.value = r.body() ?: emptyList() } catch (e: Exception) {} } }
    fun checkFollowStatus(id: Int) { viewModelScope.launch { try { val r = RetrofitClient.api.checkFollowStatus(id, getUserId()); if (r.isSuccessful) _isArtistFollowed.value = r.body()?.get("isFollowing") as? Boolean ?: false } catch (e: Exception) {} } }
    fun followArtist(id: Int) { viewModelScope.launch { try { val r = RetrofitClient.api.followArtist(id, getUserId()); if (r.isSuccessful) { val body = r.body(); _isArtistFollowed.value = body?.get("isFollowing") as? Boolean ?: false; fetchArtists() } } catch (e: Exception) {} } }
    fun checkLikeStatus(songId: Int, userId: Int) { viewModelScope.launch { try { val r = RetrofitClient.api.checkFavoriteStatus(songId, userId); if (r.isSuccessful) _isCurrentLiked.value = r.body()?.get("isFavorite") as? Boolean ?: false } catch (e: Exception) {} } }
    fun recordView(id: Int) { viewModelScope.launch { try { RetrofitClient.api.recordView(id, getUserId().let { if (it == -1) null else it }) } catch (e: Exception) {} } }
    fun likeSong(id: Int, onRes: (String) -> Unit) { viewModelScope.launch { try { val r = RetrofitClient.api.likeSong(id, getUserId()); if (r.isSuccessful) { val liked = r.body()?.get("isFavorite") as? Boolean ?: false; _isCurrentLiked.value = liked; onRes(if (liked) "Đã thích" else "Đã bỏ thích") } } catch (e: Exception) {} } }
    fun favoriteSong(id: Int) { /* logic */ }
    fun stopAndClear() { exoPlayer?.stop(); exoPlayer?.clearMediaItems(); _currentSong.value = null; _isPlaying.value = false }
}
