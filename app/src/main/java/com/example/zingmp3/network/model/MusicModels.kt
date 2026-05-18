package com.example.zingmp3.network.model

import com.example.zingmp3.network.RetrofitClient
import com.google.gson.annotations.SerializedName

data class Song(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("artist_id") val artist_id: Int? = null,
    @SerializedName("artist_name") val artist_name: String? = null,
    @SerializedName("audio_url") val audio_url: String,
    @SerializedName("image_url") val image_url: String? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("views") val views: Int = 0,
    @SerializedName("likes_count") val likes_count: Int = 0
) {
    fun getFullAudioUrl(): String {
        return if (audio_url.startsWith("http")) audio_url else RetrofitClient.BASE_URL + audio_url
    }

    fun getFullImageUrl(): String? {
        if (image_url == null) return null
        return if (image_url.startsWith("http")) image_url else RetrofitClient.BASE_URL + image_url
    }
}

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

data class Playlist(
    val id: Int,
    val name: String,
    val image_url: String? = null,
    val user_id: Int
)
