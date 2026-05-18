package com.example.zingmp3.ui.viewmodel

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.example.zingmp3.network.RetrofitClient
import com.example.zingmp3.network.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

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

    private var exoPlayer: ExoPlayer? = null

    init {
        setupPlayer()
        fetchSongs()
        updateProgress()
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer() {
        // Cấu hình LoadControl cực mạnh để tải nhạc nhanh hơn
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50000, // Tải trước 50 giây
                100000, // Tối đa 100 giây
                1500,  // Cần 1.5 giây để bắt đầu
                3000   // Cần 3 giây để phát lại sau khi lag
            )
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
                        android.util.Log.d("MusicPlayer", "State changed to: $state")
                    }
                    override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                        _isPlaying.value = isPlayingNow
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        android.util.Log.e("MusicPlayer", "Error: ${error.message}")
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
                    _songs.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Exception fetching songs", e)
            }
        }
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.fromUri(song.getFullAudioUrl())
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
