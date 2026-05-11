package com.example.commerce.payment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class TossConfirmRequest(
    @field:NotBlank val paymentKey: String,
    @field:NotNull val orderId: Long,
    @field:NotNull val amount: Long,
)
