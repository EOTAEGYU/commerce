package com.example.commerce.settlement.dto

import com.example.commerce.settlement.entity.Settlement
import com.example.commerce.settlement.entity.SettlementStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class SettlementResponse(
    val id: Long,
    val settlementDate: LocalDate,
    val status: SettlementStatus,
    val orderCount: Int,
    val totalOrderAmount: Long,
    val totalDiscountAmount: Long,
    val totalPointAmount: Long,
    val totalNetAmount: Long,
    val totalEarnedPoints: Long,
    val confirmedAt: LocalDateTime?,
    val paidAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(settlement: Settlement): SettlementResponse = SettlementResponse(
            id = settlement.id,
            settlementDate = settlement.settlementDate,
            status = settlement.status,
            orderCount = settlement.orderCount,
            totalOrderAmount = settlement.totalOrderAmount,
            totalDiscountAmount = settlement.totalDiscountAmount,
            totalPointAmount = settlement.totalPointAmount,
            totalNetAmount = settlement.totalNetAmount,
            totalEarnedPoints = settlement.totalEarnedPoints,
            confirmedAt = settlement.confirmedAt,
            paidAt = settlement.paidAt,
            createdAt = settlement.createdAt,
        )
    }
}
