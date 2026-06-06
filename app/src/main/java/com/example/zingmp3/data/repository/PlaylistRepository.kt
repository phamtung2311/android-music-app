package com.example.zingmp3.data.repository

import com.example.zingmp3.network.RetrofitClient
import com.example.zingmp3.network.model.*
import retrofit2.Response

class PlaylistRepository {

    suspend fun getPlaylists(userId: Int): Response<List<Playlist>> {
        return RetrofitClient.api.getPlaylists(userId)
    }

    suspend fun createPlaylist(name: String, userId: Int): Response<Playlist> {
        return RetrofitClient.api.createPlaylist(CreatePlaylistRequest(name, userId))
    }

    suspend fun getPlaylistDetails(playlistId: Int): Response<PlaylistDetail> {
        return RetrofitClient.api.getPlaylistDetails(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Int, songId: Int): Response<Map<String, Any>> {
        return RetrofitClient.api.addSongToPlaylist(playlistId, AddSongRequest(songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Int, songId: Int): Response<Map<String, Any>> {
        return RetrofitClient.api.removeSongFromPlaylist(playlistId, songId)
    }
}
