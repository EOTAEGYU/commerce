package com.example.commerce.review.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ReviewCreateRequest(
    val orderItemId: Long,

    @field:DecimalMin("0.5")
    @field:DecimalMax("5.0")
    val rating: Double,

    @field:NotBlank
    @field:Size(max = 500)
    val content: String,
)

data class ReviewUpdateRequest(
    @field:DecimalMin("0.5")
    @field:DecimalMax("5.0")
    val rating: Double,

    @field:NotBlank
    @field:Size(max = 500)
    val content: String,
)
