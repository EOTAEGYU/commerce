package com.example.commerce.product.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class ProductCreateRequest(
    @field:NotBlank
    val name: String,

    val description: String? = null,

    @field:Min(0)
    val price: Long,

    val categoryId: Long,

    @field:NotEmpty
    @field:Valid
    val options: List<ProductOptionRequest>,
)

data class ProductOptionRequest(
    @field:NotBlank
    val size: String,

    @field:NotBlank
    val color: String,

    @field:Min(0)
    val stock: Int,
)
