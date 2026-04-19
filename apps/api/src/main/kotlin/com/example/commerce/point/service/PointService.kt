package com.example.commerce.point.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.order.entity.Order
import com.example.commerce.point.dto.PointBalanceResponse
import com.example.commerce.point.dto.PointHistoryResponse
import com.example.commerce.point.entity.PointHistory
import com.example.commerce.point.entity.PointHistoryType
import com.example.commerce.point.entity.UserPoint
import com.example.commerce.point.repository.PointHistoryRepository
import com.example.commerce.point.repository.UserPointRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PointService(
    private val userPointRepository: UserPointRepository,
    private val pointHistoryRepository: PointHistoryRepository,
) {
    fun getOrCreateUserPoint(userId: Long): UserPoint {
        return userPointRepository.findByUserId(userId)
            ?: userPointRepository.save(UserPoint.create(userId))
    }

    fun getBalance(userId: Long): PointBalanceResponse {
        val userPoint = getOrCreateUserPoint(userId)
        return PointBalanceResponse.from(userPoint)
    }

    fun getHistory(userId: Long, pageable: Pageable): Page<PointHistoryResponse> {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map { PointHistoryResponse.from(it) }
    }

    @Transactional
    fun earnPoints(
        userId: Long,
        type: PointHistoryType,
        amount: Long,
        relatedId: Long? = null,
        description: String? = null,
    ) {
        if (amount <= 0) return

        val userPoint = getOrCreateUserPoint(userId)
        userPoint.balance += amount

        pointHistoryRepository.save(
            PointHistory.of(
                userId = userId,
                type = type,
                amount = amount,
                balance = userPoint.balance,
                relatedId = relatedId,
                description = description,
            )
        )
    }

    @Transactional
    fun earnReviewPoints(userId: Long, reviewId: Long) {
        earnPoints(userId, PointHistoryType.EARN_REVIEW, 300L, reviewId, "리뷰 작성 적립")
    }

    @Transactional
    fun earnPurchasePoints(userId: Long, orderId: Long, paymentAmount: Long) {
        val amount = paymentAmount / 100
        earnPoints(userId, PointHistoryType.EARN_PURCHASE, amount, orderId, "구매 적립")
    }

    @Transactional
    fun validateAndUsePoints(userId: Long, pointAmount: Long, order: Order): Long {
        if (pointAmount <= 0) return 0L

        if (pointAmount < 1000) throw CustomException(ErrorCode.POINT_BELOW_MINIMUM)

        val maxUsable = order.totalAmount * 50 / 100
        if (pointAmount > maxUsable) throw CustomException(ErrorCode.POINT_EXCEEDS_MAXIMUM)

        val userPoint = getOrCreateUserPoint(userId)
        if (userPoint.balance < pointAmount) throw CustomException(ErrorCode.POINT_INSUFFICIENT)

        userPoint.balance -= pointAmount

        pointHistoryRepository.save(
            PointHistory.of(
                userId = userId,
                type = PointHistoryType.USE_PAYMENT,
                amount = pointAmount,
                balance = userPoint.balance,
                relatedId = order.id,
                description = "결제 사용",
            )
        )

        return pointAmount
    }

    @Transactional
    fun refundPoints(userId: Long, orderId: Long) {
        val history = pointHistoryRepository.findByUserIdAndRelatedIdAndType(
            userId, orderId, PointHistoryType.USE_PAYMENT,
        ) ?: return

        val userPoint = getOrCreateUserPoint(userId)
        userPoint.balance += history.amount

        pointHistoryRepository.save(
            PointHistory.of(
                userId = userId,
                type = PointHistoryType.REFUND_PAYMENT,
                amount = history.amount,
                balance = userPoint.balance,
                relatedId = orderId,
                description = "결제 취소 환불",
            )
        )
    }
}
