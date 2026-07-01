package com.example.zingmp3.network.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val role: String? = "user",
    val avatar_url: String? = null,
    val bio: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class LoginRequest(
    val login: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String? = null,
    val user: User? = null
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class RegisterResponse(
    val message: String
)

data class UpdateProfileRequest(
    val username: String,
    val bio: String? = null,
    val avatar_url: String? = null
)
