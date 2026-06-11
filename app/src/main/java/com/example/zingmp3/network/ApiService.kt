package com.example.zingmp3.network

import com.example.zingmp3.network.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("songs")
    suspend fun getSongs(): Response<List<Song>>

    @PUT("profile/{id}")
    suspend fun updateProfile(
        @Path("id") id: Int,
        @Body request: UpdateProfileRequest
    ): Response<RegisterResponse>

    // API Like (Toggle) - Thử nhận userId từ Query
    @POST("songs/{id}/like")
    suspend fun likeSong(
        @Path("id") songId: Int,
        @Query("userId") userId: Int
    ): Response<Map<String, Any>>

    // API Thêm vào danh sách yêu thích
    @POST("songs/{id}/favorite")
    suspend fun favoriteSong(
        @Path("id") songId: Int,
        @Query("userId") userId: Int
    ): Response<Map<String, Any>>

    // API Ghi nhận lượt xem
    @POST("songs/{id}/view")
    suspend fun recordView(
        @Path("id") songId: Int,
        @Query("userId") userId: Int?
    ): Response<Map<String, Any>>

    @GET("songs/top-weekly")
    suspend fun getTopWeeklySongs(): Response<List<Song>>

    @GET("genres")
    suspend fun getGenres(): Response<List<Genre>>

    @GET("api/artists")
    suspend fun getArtists(): Response<List<Artist>>

    @GET("api/artists/{id}/songs")
    suspend fun getArtistSongs(@Path("id") artistId: Int): Response<List<Song>>

    @POST("api/artists/{id}/follow")
    suspend fun followArtist(
        @Path("id") artistId: Int,
        @Query("userId") userId: Int
    ): Response<Map<String, Any>>

    @GET("api/artists/{id}/check-follow")
    suspend fun checkFollowStatus(
        @Path("id") artistId: Int,
        @Query("userId") userId: Int
    ): Response<Map<String, Any>>

    // API Kiểm tra trạng thái
    @GET("songs/{id}/check-favorite")
    suspend fun checkFavoriteStatus(
        @Path("id") songId: Int,
        @Query("userId") userId: Int
    ): Response<Map<String, Any>>

    // Playlists
    @GET("api/playlists")
    suspend fun getPlaylists(@Query("userId") userId: Int): Response<List<Playlist>>

    @POST("api/playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistRequest): Response<Playlist>

    @GET("api/playlists/{id}")
    suspend fun getPlaylistDetails(@Path("id") playlistId: Int): Response<PlaylistDetail>

    @PUT("api/playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") playlistId: Int,
        @Body request: UpdatePlaylistRequest
    ): Response<Playlist>

    @DELETE("api/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") playlistId: Int): Response<Map<String, Any>>

    @POST("api/playlists/{id}/songs")
    suspend fun addSongToPlaylist(
        @Path("id") playlistId: Int,
        @Body request: AddSongRequest
    ): Response<Map<String, Any>>

    @DELETE("api/playlists/{id}/songs/{songId}")
    suspend fun removeSongFromPlaylist(
        @Path("id") playlistId: Int,
        @Path("songId") songId: Int
    ): Response<Map<String, Any>>
}
