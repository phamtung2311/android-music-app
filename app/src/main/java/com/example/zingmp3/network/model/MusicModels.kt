package com.example.zingmp3.network.model

import com.example.zingmp3.network.RetrofitClient
import com.google.gson.annotations.SerializedName
import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String? = null,
    @SerializedName("artist_id") val artist_id: Int? = null,
    @SerializedName("artist_name") val artist_name: String? = null,
    @SerializedName("audio_url") val audio_url: String? = null,
    @SerializedName("image_url") val image_url: String? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("views") val views: Int = 0,
    @SerializedName("likes_count") val likes_count: Int = 0,
    @SerializedName("is_public") val isPublic: Boolean = true
) {
    fun getFullAudioUrl(): String {
        val url = audio_url ?: ""
        return if (url.startsWith("http")) url else RetrofitClient.BASE_URL + url
    }

    fun getFullImageUrl(): String? {
        if (image_url == null) return null
        return if (image_url.startsWith("http")) image_url else RetrofitClient.BASE_URL + image_url
    }
}

@Immutable
data class Artist(
    val id: Int,
    val stage_name: String,
    val avatar_url: String? = null,
    val followers_count: Int = 0,
    val is_verified: Boolean = false
) {
    fun getFullAvatarUrl(): String? {
        if (avatar_url == null) return null
        return if (avatar_url.startsWith("http")) avatar_url else RetrofitClient.BASE_URL + avatar_url
    }
}

@Immutable
data class Playlist(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("image_url") val image_url: String? = null,
    @SerializedName("user_id") val user_id: Int,
    @SerializedName("songs_count") val songsCount: Int = 0
)

data class CreatePlaylistRequest(
    @SerializedName("name") val name: String,
    @SerializedName("user_id") val userId: Int
)

data class AddSongRequest(
    @SerializedName("song_id") val songId: Int
)

data class PlaylistDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("songs") val songs: List<Song> = emptyList()
)

@Immutable
data class Genre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null
)
