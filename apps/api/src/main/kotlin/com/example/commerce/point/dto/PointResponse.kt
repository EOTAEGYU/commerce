package com.example.commerce.point.dto

import com.example.commerce.point.entity.PointHistory
import com.example.commerce.point.entity.PointHistoryType
import com.example.commerce.point.entity.UserPoint
import java.time.LocalDateTime

data class PointBalanceResponse(
    val balance: Long,
) {
    companion object {
        fun from(userPoint: UserPoint): PointBalanceResponse = PointBalanceResponse(
            balance = userPoint.balance,
        )
    }
}

data class PointHistoryResponse(
    val id: Long,
    val type: PointHistoryType,
    val typeDescription: String,
    val amount: Long,
    val balance: Long,
    val relatedId: Long?,
    val description: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(history: PointHistory): PointHistoryResponse = PointHistoryResponse(
            id = history.id,
            type = history.type,
            typeDescription = history.type.description,
            amount = history.amount,
            balance = history.balance,
            relatedId = history.relatedId,
            description = history.description,
            createdAt = history.createdAt,
        )
    }
}
