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

/**
 * MusicViewModel: Lớp trung tâm quản lý dữ liệu nhạc, trạng thái trình phát và tương tác với UI.
 * Kế thừa AndroidViewModel để có thể truy cập Application Context (cần cho SharedPreferences và Service).
 */
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    // SharedPreferences dùng để lưu trữ dữ liệu nhỏ lẻ như cài đặt, lịch sử, cache...
    private val sharedPref = application.getSharedPreferences("USER_DATA", android.content.Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- Private StateFlows (Quản lý trạng thái nội bộ) ---
    private val _songs = MutableStateFlow<List<Song>>(loadCachedSongs()) // Danh sách tất cả bài hát
    private val _rawGenres = MutableStateFlow<List<String>>(emptyList()) // Danh sách thể loại từ API
    private val _selectedGenre = MutableStateFlow("All") // Thể loại đang được chọn
    private val _topWeeklySongs = MutableStateFlow<List<Song>>(emptyList()) // Top bài hát tuần
    private val _username = MutableStateFlow("User") // Tên người dùng
    private val _artists = MutableStateFlow<List<Artist>>(emptyList()) // Danh sách nghệ sĩ
    private val _artistSongs = MutableStateFlow<List<Song>>(emptyList()) // Bài hát của một nghệ sĩ cụ thể
    private val _currentSong = MutableStateFlow<Song?>(null) // Bài hát đang phát hiện tại
    private val _isPlaying = MutableStateFlow(false) // Trạng thái đang phát hay tạm dừng
    private val _currentPosition = MutableStateFlow(0L) // Vị trí thời gian hiện tại của bài hát
    private val _duration = MutableStateFlow(0L) // Tổng thời lượng bài hát hiện tại
    private val _isBuffering = MutableStateFlow(false) // Trạng thái đang tải dữ liệu (buffering)
    private val _isCurrentLiked = MutableStateFlow(false) // Bài hát hiện tại có được thích không
    private val _isArtistFollowed = MutableStateFlow(false) // Nghệ sĩ hiện tại có được theo dõi không
    private val _followedArtistIds = MutableStateFlow<Set<Int>>(loadFollowedArtistIds()) // Tập hợp ID nghệ sĩ đã follow
    private val _historyIds = MutableStateFlow<List<Int>>(emptyList()) // Lịch sử nghe nhạc (ID)
    private val _searchQuery = MutableStateFlow("") // Từ khóa tìm kiếm hiện tại
    private val _searchHistory = MutableStateFlow<List<String>>(loadSearchHistory()) // Lịch sử tìm kiếm
    private val _isShuffleEnabled = MutableStateFlow(false) // Trạng thái phát ngẫu nhiên
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF) // Chế độ lặp lại
    private val _downloadedSongs = MutableStateFlow<Set<Int>>(emptySet()) // Tập hợp ID các bài hát đã tải
    private val _downloadedSongsMetadata = MutableStateFlow<List<Song>>(loadDownloadedMetadata()) // Thông tin các bài đã tải

    // --- Public StateFlows (Cung cấp dữ liệu cho UI lắng nghe) ---
    
    // Danh sách thể loại, tự động sắp xếp dựa trên số lượng bài hát thuộc thể loại đó
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
    
    // Lọc danh sách bài hát theo thể loại đang chọn
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

    // Danh sách các nghệ sĩ mà người dùng đang theo dõi
    val followedArtists: StateFlow<List<Artist>> = combine(_artists, _followedArtistIds) { allArtists, followedIds ->
        allArtists.filter { followedIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Kết quả tìm kiếm bài hát theo từ khóa
    val searchResultsSongs: StateFlow<List<Song>> = combine(_songs, _searchQuery) { list, query ->
        if (query.isBlank()) emptyList() else list.filter { it.title?.contains(query, true) == true || it.artist_name?.contains(query, true) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Kết quả tìm kiếm nghệ sĩ theo từ khóa
    val searchResultsArtists: StateFlow<List<Artist>> = combine(_artists, _searchQuery) { list, query ->
        if (query.isBlank()) emptyList() else list.filter { it.stage_name.contains(query, true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Gợi ý bài hát và nghệ sĩ
    val recommendedSongs: StateFlow<List<Song>> = _songs.map { it.shuffled().take(10) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recommendedArtists: StateFlow<List<Artist>> = _artists.map { it.sortedByDescending { a -> a.followers_count }.take(10) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var exoPlayer: ExoPlayer? = null
    private var isPlayerListenerAdded = false
    private var statsJob: Job? = null

    init {
        // Đồng bộ ID bài hát đã tải vào Set để tra cứu nhanh
        _downloadedSongs.value = _downloadedSongsMetadata.value.map { it.id }.toSet()
        refreshData() // Lấy dữ liệu mới từ API
        loadUsername() // Load tên user từ bộ nhớ
        setupPlayer() // Khởi tạo trình phát nhạc
        restorePlaybackState() // Khôi phục trạng thái phát nhạc cuối cùng (nếu có)
    }

    // --- Persistence & Metadata Logic (Xử lý lưu trữ dữ liệu cục bộ) ---

    // Load thông tin bài hát đã tải xuống từ SharedPreferences
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

    // Load danh sách bài hát đã cache để hiển thị ngay khi mở app (Offline mode/Fast load)
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

    // --- Player Core (Cấu hình và điều khiển ExoPlayer) ---

    private fun setupPlayer() {
        // Lấy đối tượng ExoPlayer duy nhất từ PlayerManager (Singleton)
        exoPlayer = PlayerManager.getPlayer(getApplication())
        exoPlayer?.let { player ->
            if (!isPlayerListenerAdded) {
                // Đăng ký lắng nghe các sự kiện từ Player
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        _isBuffering.value = state == Player.STATE_BUFFERING
                        if (state == Player.STATE_READY) _duration.value = player.duration
                        if (state == Player.STATE_ENDED) _isPlaying.value = false
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // Khi chuyển bài hát, cập nhật thông tin bài hiện tại lên UI
                        val currentQueue = PlayerManager.currentPlayQueue
                        val index = player.currentMediaItemIndex
                        if (index in currentQueue.indices) {
                            val song = currentQueue[index]
                            _currentSong.value = song
                            checkLikeStatus(song.id, getUserId())
                            savePlaybackState() // Lưu lại vị trí để sau này khôi phục
                            recordView(song.id) // Ghi nhận lượt nghe
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) updateProgress() // Nếu đang phát thì bắt đầu cập nhật thanh tiến trình
                    }
                    
                    override fun onPlayerError(error: PlaybackException) {
                        // Xử lý lỗi: Tự động chuyển bài nếu bài hiện tại lỗi
                        player.seekToNext()
                        player.prepare()
                        player.play()
                    }
                })
                isPlayerListenerAdded = true
            }
            // Đồng bộ trạng thái ban đầu của player vào StateFlow
            _isShuffleEnabled.value = player.shuffleModeEnabled
            _repeatMode.value = player.repeatMode
            _isPlaying.value = player.isPlaying
        }
    }

    /**
     * Phát một danh sách bài hát bắt đầu từ vị trí startIndex.
     */
    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        
        // Khởi động Service trước khi bắt đầu phát nhạc để nhạc vẫn chạy khi thoát app
        ensureServiceRunning()
        
        PlayerManager.currentPlayQueue = songs
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            // Chuyển đổi danh sách Song sang MediaItem mà ExoPlayer hiểu được
            val mediaItems = songs.map { createMediaItem(it) }
            player.setMediaItems(mediaItems)
            player.seekTo(if (startIndex in songs.indices) startIndex else 0, 0L)
            player.prepare()
            player.play()
            savePlaybackState()
        }
    }

    /**
     * Phát một bài hát duy nhất.
     */
    fun playSong(song: Song) {
        // Nếu bài hát đang phát trùng với bài yêu cầu, chỉ cần nhấn Play
        if (_currentSong.value?.id == song.id && exoPlayer?.playbackState != Player.STATE_ENDED) {
            exoPlayer?.play()
            ensureServiceRunning()
            return
        }
        playPlaylist(listOf(song), 0)
    }

    // --- Downloads (Xử lý tải nhạc về máy) ---

    fun downloadSong(song: Song, onResult: (String) -> Unit) {
        // Lấy thư mục Music của app
        val downloadDir = getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC) ?: return
        val destinationFile = java.io.File(downloadDir, "${song.id}.mp3")
        
        // Kiểm tra xem bài hát đã tồn tại chưa
        if (destinationFile.exists()) {
            onResult("Bài hát đã có sẵn")
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Sử dụng OkHttp để tải file
                val response = okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().url(song.getFullAudioUrl()).build()).execute()
                if (response.isSuccessful) {
                    // Ghi dữ liệu vào file
                    response.body?.byteStream()?.use { input -> destinationFile.outputStream().use { output -> input.copyTo(output) } }
                    
                    // Cập nhật Metadata (danh sách bài đã tải)
                    val currentMetadata = _downloadedSongsMetadata.value.toMutableList()
                    if (currentMetadata.none { it.id == song.id }) {
                        currentMetadata.add(song)
                        _downloadedSongsMetadata.value = currentMetadata
                        saveDownloadedMetadata(currentMetadata)
                    }
                    
                    // Cập nhật tập hợp ID đã tải
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

    /**
     * Tạo MediaItem từ dữ liệu Song.
     * Ưu tiên sử dụng file đã tải xuống (local) nếu có, nếu không thì dùng URL stream (online).
     */
    private fun createMediaItem(song: Song): MediaItem {
        val downloadDir = getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
        val localFile = java.io.File(downloadDir, "${song.id}.mp3")
        val uri = if (localFile.exists()) android.net.Uri.fromFile(localFile) else android.net.Uri.parse(song.getFullAudioUrl())

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist_name)
                .setArtworkUri(android.net.Uri.parse(song.getFullImageUrl() ?: ""))
                .build())
            .build()
    }

    // --- Others (Các hàm tiện ích điều khiển nhạc và lịch sử) ---
    
    fun togglePlayPause() { exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun skipToNext() { exoPlayer?.seekToNext() }
    
    // Quay lại bài trước, nếu đã nghe quá 5s thì chỉ tua lại từ đầu bài hiện tại
    fun skipToPrevious() { if ((exoPlayer?.currentPosition ?: 0) > 5000) exoPlayer?.seekTo(0) else exoPlayer?.seekToPrevious() }
    
    fun toggleShuffle() { exoPlayer?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled; _isShuffleEnabled.value = it.shuffleModeEnabled } }
    
    fun toggleRepeatMode() {
        exoPlayer?.let {
            val next = when (it.repeatMode) { 
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL 
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE 
                else -> Player.REPEAT_MODE_OFF 
            }
            it.repeatMode = next; _repeatMode.value = next
        }
    }

    fun addToSearchHistory(query: String) {
        val current = _searchHistory.value.toMutableList()
        current.remove(query) // Xóa nếu đã tồn tại để đưa lên đầu
        current.add(0, query)
        val newHistory = current.take(10) // Chỉ giữ lại 10 mục gần nhất
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

    // Khôi phục lại hàng chờ và vị trí phát nhạc từ lần cuối sử dụng
    private fun restorePlaybackState() {
        val lastQueueJson = sharedPref?.getString("last_queue", null)
        val lastIndex = sharedPref?.getInt("last_index", -1) ?: -1
        if (lastQueueJson != null && lastIndex != -1) {
            try {
                val queue: List<Song> = gson.fromJson(lastQueueJson, object : TypeToken<List<Song>>() {}.type)
                if (queue.isNotEmpty()) {
                    PlayerManager.currentPlayQueue = queue
                    viewModelScope.launch { 
                        delay(1000) // Chờ một chút để player sẵn sàng
                        exoPlayer?.setMediaItems(queue.map { createMediaItem(it) })
                        exoPlayer?.seekTo(lastIndex, 0L)
                        exoPlayer?.prepare() 
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun savePlaybackState() {
        val queue = PlayerManager.currentPlayQueue
        val index = exoPlayer?.currentMediaItemIndex ?: -1
        if (queue.isNotEmpty() && index != -1) {
            sharedPref?.edit()
                ?.putString("last_queue", gson.toJson(queue))
                ?.putInt("last_index", index)
                ?.apply()
        }
    }

    fun loadUsername() { _username.value = sharedPref?.getString("username", "User") ?: "User" }
    fun setGenre(genre: String) { _selectedGenre.value = genre }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun refreshData() { fetchSongs(); fetchTopWeekly(); fetchArtists(); fetchGenres() }

    // --- API Calls (Tương tác với Server qua Retrofit) ---

    fun fetchArtists() { viewModelScope.launch { try { val r = RetrofitClient.api.getArtists(); if (r.isSuccessful) _artists.value = r.body() ?: emptyList() } catch (e: Exception) {} } }
    fun fetchArtistSongs(id: Int) { viewModelScope.launch { try { val r = RetrofitClient.api.getArtistSongs(id); if (r.isSuccessful) _artistSongs.value = r.body() ?: emptyList() } catch (e: Exception) {} } }
    fun fetchGenres() { viewModelScope.launch { try { val r = RetrofitClient.api.getGenres(); if (r.isSuccessful) _rawGenres.value = r.body()?.map { it.name } ?: emptyList() } catch (e: Exception) {} } }
    
    private fun getUserId(): Int = sharedPref?.getInt("userId", -1) ?: -1

    // Đảm bảo PlaybackService đang chạy để duy trì notification và điều khiển nhạc ở màn hình khóa
    private fun ensureServiceRunning() { 
        val intent = android.content.Intent(getApplication(), PlaybackService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    // Cập nhật vị trí phát nhạc (giây) mỗi 1 giây để UI hiển thị thanh SeekBar
    private fun updateProgress() { 
        viewModelScope.launch { 
            while (_isPlaying.value) { 
                try {
                    _currentPosition.value = exoPlayer?.currentPosition ?: 0L 
                } catch (e: Exception) {
                    _isPlaying.value = false
                    break
                }
                delay(1000) 
            } 
        } 
    }

    fun seekTo(pos: Long) { exoPlayer?.seekTo(pos) }
    
    fun fetchSongs() { 
        viewModelScope.launch { 
            try { 
                val r = RetrofitClient.api.getSongs()
                if (r.isSuccessful) { 
                    val list = r.body() ?: emptyList()
                    _songs.value = list
                    saveCachedSongs(list) // Lưu vào cache để dùng khi offline
                } 
            } catch (e: Exception) {} 
        } 
    }

    fun fetchTopWeekly() { viewModelScope.launch { try { val r = RetrofitClient.api.getTopWeeklySongs(); if (r.isSuccessful) _topWeeklySongs.value = r.body() ?: emptyList() } catch (e: Exception) {} } }
    fun checkFollowStatus(id: Int) { viewModelScope.launch { try { val r = RetrofitClient.api.checkFollowStatus(id, getUserId()); if (r.isSuccessful) _isArtistFollowed.value = r.body()?.get("isFollowing") as? Boolean ?: false } catch (e: Exception) {} } }
    fun followArtist(id: Int) { viewModelScope.launch { try { val r = RetrofitClient.api.followArtist(id, getUserId()); if (r.isSuccessful) { val body = r.body(); _isArtistFollowed.value = body?.get("isFollowing") as? Boolean ?: false; fetchArtists() } } catch (e: Exception) {} } }
    fun checkLikeStatus(songId: Int, userId: Int) { viewModelScope.launch { try { val r = RetrofitClient.api.checkFavoriteStatus(songId, userId); if (r.isSuccessful) _isCurrentLiked.value = r.body()?.get("isFavorite") as? Boolean ?: false } catch (e: Exception) {} } }
    
    // Gửi tín hiệu đã nghe bài hát lên server để tăng lượt view
    fun recordView(id: Int) { viewModelScope.launch { try { RetrofitClient.api.recordView(id, getUserId().let { if (it == -1) null else it }) } catch (e: Exception) {} } }

    /**
     * Xử lý thích bài hát với cơ chế Optimistic Update: 
     * Cập nhật UI ngay lập tức trước khi gọi API, nếu API lỗi thì mới hoàn tác (revert).
     */
    fun likeSong(id: Int, onRes: (String) -> Unit) {
        viewModelScope.launch {
            val previousLiked = _isCurrentLiked.value
            val currentSongVal = _currentSong.value
            
            // 1. Cập nhật UI ngay lập tức (Optimistic Update)
            if (currentSongVal?.id == id) {
                _isCurrentLiked.value = !previousLiked
                _currentSong.value = currentSongVal.copy(
                    likes_count = if (!previousLiked) currentSongVal.likes_count + 1 
                                 else (currentSongVal.likes_count - 1).coerceAtLeast(0)
                )
            }

            try {
                // 2. Gọi API thực sự
                val r = RetrofitClient.api.likeSong(id, getUserId())
                if (r.isSuccessful) {
                    val likedFromServer = r.body()?.get("isFavorite") as? Boolean ?: !previousLiked
                    _isCurrentLiked.value = likedFromServer
                    
                    // Đồng bộ số lượng like vào danh sách bài hát tổng thể
                    _songs.value = _songs.value.map { 
                        if (it.id == id) it.copy(
                            likes_count = if (likedFromServer) (if (previousLiked) it.likes_count else it.likes_count + 1)
                                          else (if (previousLiked) it.likes_count - 1 else it.likes_count).coerceAtLeast(0)
                        ) else it 
                    }
                    
                    onRes(if (likedFromServer) "Đã thích" else "Đã bỏ thích")
                } else {
                    // 3. Hoàn tác nếu API thất bại
                    if (_currentSong.value?.id == id) {
                        _isCurrentLiked.value = previousLiked
                        _currentSong.value = currentSongVal
                    }
                    onRes("Không thể thực hiện")
                }
            } catch (e: Exception) {
                // Hoàn tác nếu lỗi mạng
                if (_currentSong.value?.id == id) {
                    _isCurrentLiked.value = previousLiked
                    _currentSong.value = currentSongVal
                }
                onRes("Lỗi kết nối")
            }
        }
    }
    
    fun favoriteSong(id: Int) { /* Logic cho danh sách yêu thích khác nếu cần */ }
    
    // Dừng nhạc và xóa hàng chờ
    fun stopAndClear() { 
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _currentSong.value = null
        _isPlaying.value = false 
    }
}
