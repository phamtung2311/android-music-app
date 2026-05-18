package com.example.zingmp3.network.model

data class LoginRequest(
    val login: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val role: String? = "user"
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class RegisterResponse(
    val message: String
)
