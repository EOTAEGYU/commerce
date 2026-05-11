package com.example.commerce.payment.dto

data class KakaoInitiateResponse(
    val nextRedirectPcUrl: String,
    val orderId: Long,
)
