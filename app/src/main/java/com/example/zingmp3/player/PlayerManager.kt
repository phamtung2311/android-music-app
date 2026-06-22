package com.example.zingmp3.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.example.zingmp3.network.model.Song

object PlayerManager {
    private var exoPlayer: ExoPlayer? = null
    var currentPlayQueue: List<Song> = emptyList()

    fun getPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context.applicationContext).build()
        }
        return exoPlayer!!
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
