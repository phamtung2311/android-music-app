package com.example.zingmp3.data.repository

import com.example.zingmp3.network.RetrofitClient
import com.example.zingmp3.network.model.GoogleLoginRequest
import com.example.zingmp3.network.model.LoginRequest
import com.example.zingmp3.network.model.RegisterRequest

class AuthRepository {
    private val api = RetrofitClient.api

    suspend fun login(request: LoginRequest) = api.login(request)
    suspend fun loginWithGoogle(request: GoogleLoginRequest) = api.loginWithGoogle(request)
    suspend fun register(request: RegisterRequest) = api.register(request)
    suspend fun updateProfile(id: Int, request: com.example.zingmp3.network.model.UpdateProfileRequest) = api.updateProfile(id, request)
}
