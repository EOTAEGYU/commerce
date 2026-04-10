package com.example.commerce.payment.dto

import com.example.commerce.payment.entity.PaymentMethod
import jakarta.validation.constraints.NotNull

data class PaymentRequest(
    @field:NotNull val orderId: Long,
    @field:NotNull val method: PaymentMethod,
    val simulateFailure: Boolean = false,
)
