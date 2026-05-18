package com.example.zingmp3.network

import com.example.zingmp3.network.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @retrofit2.http.GET("songs")
    suspend fun getSongs(): Response<List<Song>>
}
