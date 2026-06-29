package com.example.zingmp3.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.example.zingmp3.network.model.Song

object PlayerManager {
    private var exoPlayer: ExoPlayer? = null
    var currentPlayQueue: List<Song> = emptyList()

    fun getPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer = ExoPlayer.Builder(context.applicationContext)
                .setAudioAttributes(audioAttributes, true) // true để tự động quản lý AudioFocus
                .setHandleAudioBecomingNoisy(true) // Tự dừng khi rút tai nghe
                .build()
        }
        return exoPlayer!!
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
