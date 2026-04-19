package com.example.commerce.category.dto

import jakarta.validation.constraints.NotBlank

data class CategoryUpdateRequest(
    @field:NotBlank
    val name: String,
    val displayOrder: Int = 0,
)
