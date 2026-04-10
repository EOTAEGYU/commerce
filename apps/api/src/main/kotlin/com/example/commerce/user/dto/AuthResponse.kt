package com.example.commerce.user.dto

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
)
