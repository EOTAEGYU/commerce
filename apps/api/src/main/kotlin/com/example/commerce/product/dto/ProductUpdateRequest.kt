package com.example.commerce.product.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class ProductUpdateRequest(
    @field:NotBlank
    val name: String,

    val description: String? = null,

    @field:Min(0)
    val price: Long,

    val categoryId: Long,

    val imageUrl: String? = null,

    @field:Valid
    val options: List<ProductOptionRequest>? = null,
)
