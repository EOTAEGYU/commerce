package com.example.commerce.payment.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class PgReadyRequest(
    @field:NotNull val orderId: Long,
    val couponId: Long? = null,
    @field:Min(0) val pointAmount: Long? = null,
)
