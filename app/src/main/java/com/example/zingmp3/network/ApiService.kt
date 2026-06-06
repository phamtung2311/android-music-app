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

    // API Kiểm tra trạng thái
    @GET("songs/{id}/check-favorite")
    suspend fun checkFavoriteStatus(
        @Path("id") songId: Int,
        @Query("userId") userId: Int
    ): Response<Map<String, Any>>
}
