package com.example.commerce.payment.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.coupon.service.CouponService
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.order.repository.OrderRepository
import com.example.commerce.payment.dto.PaymentRequest
import com.example.commerce.payment.dto.PaymentResponse
import com.example.commerce.payment.entity.Payment
import com.example.commerce.payment.entity.PaymentStatus
import com.example.commerce.payment.repository.PaymentRepository
import com.example.commerce.point.service.PointService
import com.example.commerce.product.repository.ProductOptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class PaymentService(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val productOptionRepository: ProductOptionRepository,
    private val couponService: CouponService,
    private val pointService: PointService,
) {
    fun requestPayment(userId: Long, request: PaymentRequest): PaymentResponse {
        val order = orderRepository.findById(request.orderId)
            .orElseThrow { CustomException(ErrorCode.ORDER_NOT_FOUND) }
        if (order.userId != userId) throw CustomException(ErrorCode.ORDER_NOT_OWNED)
        if (order.status != OrderStatus.PENDING) throw CustomException(ErrorCode.ORDER_NOT_PAYABLE)

        val existingPayment = paymentRepository.findByOrderId(order.id)
        if (existingPayment?.status == PaymentStatus.COMPLETED) throw CustomException(ErrorCode.ORDER_ALREADY_PAID)

        val (discountAmount, usedCouponId) = if (request.couponId != null) {
            couponService.validateAndApplyCoupon(userId, request.couponId, order)
        } else {
            0L to null
        }

        val pointUsed = pointService.validateAndUsePoints(userId, request.pointAmount ?: 0L, order)

        val finalAmount = order.totalAmount - discountAmount - pointUsed

        val payment = paymentRepository.save(
            Payment(
                orderId = order.id,
                userId = userId,
                amount = finalAmount,
                discountAmount = discountAmount,
                couponId = usedCouponId,
                pointAmount = pointUsed,
                method = request.method,
            )
        )

        if (request.simulateFailure) {
            order.items.forEach { item ->
                val option = productOptionRepository.findByIdWithLock(item.productOptionId)
                    ?: throw CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND)
                option.stock += item.quantity
            }
            pointService.refundPoints(userId, order.id)
            order.status = OrderStatus.CANCELLED
            payment.status = PaymentStatus.FAILED
        } else {
            order.status = OrderStatus.PAID
            payment.status = PaymentStatus.COMPLETED
            payment.pgTransactionId = UUID.randomUUID().toString()
            if (usedCouponId != null) {
                couponService.markAsUsed(usedCouponId, order.id)
            }
            pointService.earnPurchasePoints(userId, order.id, finalAmount)
            val earnedPoints = finalAmount / 100
            payment.earnedPoints = earnedPoints
        }

        return PaymentResponse.from(payment, order.totalAmount, payment.earnedPoints)
    }

    @Transactional(readOnly = true)
    fun getPayment(userId: Long, orderId: Long): PaymentResponse {
        val payment = paymentRepository.findByOrderId(orderId)
            ?: throw CustomException(ErrorCode.PAYMENT_NOT_FOUND)
        if (payment.userId != userId) throw CustomException(ErrorCode.ORDER_NOT_OWNED)
        val originalAmount = payment.amount + payment.discountAmount + payment.pointAmount
        return PaymentResponse.from(payment, originalAmount, payment.earnedPoints)
    }
}
