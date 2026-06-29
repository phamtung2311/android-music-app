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
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("views") val views: Int = 0,
    @SerializedName("likes_count") val likes_count: Int = 0,
    @SerializedName("lyrics") val lyrics: String? = null,
    @SerializedName("is_public") val isPublic: Boolean = true
) {
    fun getFullAudioUrl(): String {
        val url = audio_url ?: ""
        return if (url.startsWith("http")) url else RetrofitClient.BASE_URL + url
    }

    fun getFullImageUrl(): String? {
        if (image_url == null) return null
        
        // Nếu là link tuyệt đối, lấy phần sau port 5000 để dùng với IP hiện tại
        val path = if (image_url.startsWith("http")) {
            image_url.substringAfter(":5000/")
        } else {
            image_url
        }
        
        return RetrofitClient.BASE_URL + path.removePrefix("/")
    }
}

@Immutable
data class Artist(
    @SerializedName("id") val id: Int,
    @SerializedName("stage_name") val stage_name: String,
    @SerializedName("avatar_url") val avatar_url: String? = null,
    @SerializedName("followers_count") val followers_count: Int = 0,
    @SerializedName("is_verified") val is_verified: Boolean = false
) {
    fun getFullAvatarUrl(): String? {
        if (avatar_url == null) return null
        
        // Xử lý nếu avatar_url là link tuyệt đối nhưng sai IP
        var path = if (avatar_url.startsWith("http")) {
            avatar_url.substringAfter(":5000/")
        } else {
            avatar_url
        }
        
        // Sửa lỗi đường dẫn: Nếu file nằm ở uploads/ nhưng DB lưu uploads/images/
        // Dựa trên ảnh VS Code của người dùng, ảnh nghệ sĩ nằm ở uploads/ trực tiếp
        if (path.startsWith("uploads/images/17811")) {
            path = path.replace("uploads/images/", "uploads/")
        }

        return if (path.startsWith("http")) path else RetrofitClient.BASE_URL + path.removePrefix("/")
    }
}

@Immutable
data class Playlist(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("image_url") val image_url: String? = null,
    @SerializedName("user_id") val user_id: Int,
    @SerializedName("songs_count") val songsCount: Int = 0,
    @SerializedName("song_count") val songCountFallback: Int = 0
) {
    val realSongsCount: Int
        get() = if (songsCount > 0) songsCount else songCountFallback

    fun getFullImageUrl(): String? {
        if (image_url == null) return null
        
        // Nếu là link tuyệt đối, lấy phần sau port 5000 để dùng với IP hiện tại
        val path = if (image_url.startsWith("http")) {
            image_url.substringAfter(":5000/")
        } else {
            image_url
        }
        
        return RetrofitClient.BASE_URL + path.removePrefix("/")
    }
}

data class CreatePlaylistRequest(
    @SerializedName("name") val name: String,
    @SerializedName("user_id") val userId: Int
)

data class UpdatePlaylistRequest(
    @SerializedName("name") val name: String
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
