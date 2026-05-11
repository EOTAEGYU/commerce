package com.example.commerce.payment.dto

data class TossReadyResponse(
    val amount: Long,
    val orderId: Long,
    val orderName: String,
)
