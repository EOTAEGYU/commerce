package com.example.commerce.settlement.dto

import java.time.LocalDate

data class SettlementStatsResponse(
    val from: LocalDate,
    val to: LocalDate,
    val totalDays: Int,
    val settledDays: Int,
    val totalOrderCount: Int,
    val totalOrderAmount: Long,
    val totalDiscountAmount: Long,
    val totalPointAmount: Long,
    val totalNetAmount: Long,
    val totalEarnedPoints: Long,
    val avgDailyNetAmount: Long,
)
