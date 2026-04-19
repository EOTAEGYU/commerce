package com.example.commerce.user.dto

import jakarta.validation.constraints.NotBlank

data class UpdateProfileRequest(
    @field:NotBlank val name: String,
)
