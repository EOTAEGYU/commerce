package com.example.commerce.cart.dto

import jakarta.validation.constraints.Min

data class CartItemUpdateRequest(
    @field:Min(1) val quantity: Int,
)
