package com.example.commerce.cart.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class CartItemAddRequest(
    @field:NotNull val productId: Long,
    @field:NotNull val productOptionId: Long,
    @field:Min(1) val quantity: Int,
)
