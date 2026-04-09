package com.example.commerce.category.dto

import jakarta.validation.constraints.NotBlank

data class CategoryCreateRequest(
    @field:NotBlank
    val name: String,
    val parentId: Long? = null,
    val displayOrder: Int = 0,
)
