package com.example.commerce.payment.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.coupon.service.CouponService
import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.order.repository.OrderRepository
import com.example.commerce.payment.config.PgProperties
import com.example.commerce.payment.dto.KakaoInitiateResponse
import com.example.commerce.payment.dto.PaymentRequest
import com.example.commerce.payment.dto.PaymentResponse
import com.example.commerce.payment.dto.PgReadyRequest
import com.example.commerce.payment.dto.TossConfirmRequest
import com.example.commerce.payment.dto.TossReadyResponse
import com.example.commerce.payment.entity.Payment
import com.example.commerce.payment.entity.PaymentMethod
import com.example.commerce.payment.entity.PaymentStatus
import com.example.commerce.payment.entity.PgPaymentSession
import com.example.commerce.payment.entity.PgSessionStatus
import com.example.commerce.payment.pg.KakaoPayClient
import com.example.commerce.payment.pg.TossPayClient
import com.example.commerce.payment.repository.PaymentRepository
import com.example.commerce.payment.repository.PgPaymentSessionRepository
import com.example.commerce.point.service.PointService
import com.example.commerce.product.repository.ProductOptionRepository
import org.springframework.beans.factory.annotation.Value
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
    private val pgPaymentSessionRepository: PgPaymentSessionRepository,
    private val kakaoPayClient: KakaoPayClient,
    private val tossPayClient: TossPayClient,
    private val pgProperties: PgProperties,
    @Value("\${app.frontend-url}") private val frontendUrl: String,
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

    fun initiateKakaoPayment(userId: Long, request: PgReadyRequest): KakaoInitiateResponse {
        val order = orderRepository.findById(request.orderId)
            .orElseThrow { CustomException(ErrorCode.ORDER_NOT_FOUND) }
        if (order.userId != userId) throw CustomException(ErrorCode.ORDER_NOT_OWNED)
        if (order.status != OrderStatus.PENDING) throw CustomException(ErrorCode.ORDER_NOT_PAYABLE)

        // 기존 PENDING 세션 있으면 삭제 (재시도 허용)
        val existingSession = pgPaymentSessionRepository.findByOrderId(order.id)
        if (existingSession != null) pgPaymentSessionRepository.delete(existingSession)

        val (discountAmount, _) = if (request.couponId != null) {
            couponService.validateAndApplyCoupon(userId, request.couponId, order)
        } else {
            0L to null
        }

        val pointAmount = request.pointAmount ?: 0L
        if (pointAmount > 0) validatePointAmount(userId, pointAmount, order)

        val finalAmount = order.totalAmount - discountAmount - pointAmount
        val itemName = order.items.firstOrNull()?.productName ?: "상품"

        val readyResponse = kakaoPayClient.ready(
            KakaoPayClient.ReadyRequest(
                cid = pgProperties.kakao.cid,
                partnerOrderId = order.id.toString(),
                partnerUserId = userId.toString(),
                itemName = itemName,
                quantity = 1,
                totalAmount = finalAmount,
                taxFreeAmount = 0,
                approvalUrl = "$frontendUrl/payment/kakao/approve?orderId=${order.id}",
                cancelUrl = "$frontendUrl/payment/kakao/cancel",
                failUrl = "$frontendUrl/payment/kakao/fail",
            )
        )

        pgPaymentSessionRepository.save(
            PgPaymentSession(
                orderId = order.id,
                userId = userId,
                pgType = PaymentMethod.KAKAO_PAY,
                tid = readyResponse.tid,
                amount = finalAmount,
                discountAmount = discountAmount,
                couponId = request.couponId,
                pointAmount = pointAmount,
            )
        )

        return KakaoInitiateResponse(
            nextRedirectPcUrl = readyResponse.nextRedirectPcUrl,
            orderId = order.id,
        )
    }

    fun approveKakaoPayment(userId: Long, pgToken: String, orderId: Long): PaymentResponse {
        val session = pgPaymentSessionRepository.findByOrderIdAndStatus(orderId, PgSessionStatus.PENDING)
            ?: throw CustomException(ErrorCode.PG_SESSION_NOT_FOUND)
        if (session.userId != userId) throw CustomException(ErrorCode.ORDER_NOT_OWNED)

        val approveResponse = kakaoPayClient.approve(
            KakaoPayClient.ApproveRequest(
                cid = pgProperties.kakao.cid,
                tid = session.tid!!,
                partnerOrderId = orderId.toString(),
                partnerUserId = userId.toString(),
                pgToken = pgToken,
            )
        )

        val order = orderRepository.findById(orderId)
            .orElseThrow { CustomException(ErrorCode.ORDER_NOT_FOUND) }
        if (order.status != OrderStatus.PENDING) throw CustomException(ErrorCode.ORDER_NOT_PAYABLE)

        if (session.pointAmount > 0) {
            pointService.validateAndUsePoints(userId, session.pointAmount, order)
        }

        val payment = paymentRepository.save(
            Payment(
                orderId = order.id,
                userId = userId,
                amount = session.amount,
                discountAmount = session.discountAmount,
                couponId = session.couponId,
                pointAmount = session.pointAmount,
                method = PaymentMethod.KAKAO_PAY,
                status = PaymentStatus.COMPLETED,
                pgTransactionId = approveResponse.tid,
            )
        )

        order.status = OrderStatus.PAID

        val kakaoCouponId = session.couponId
        if (kakaoCouponId != null) {
            couponService.markAsUsed(kakaoCouponId, order.id)
        }
        pointService.earnPurchasePoints(userId, order.id, session.amount)
        payment.earnedPoints = session.amount / 100

        session.status = PgSessionStatus.COMPLETED

        return PaymentResponse.from(payment, order.totalAmount, payment.earnedPoints)
    }

    fun initiateTossPayment(userId: Long, request: PgReadyRequest): TossReadyResponse {
        val order = orderRepository.findById(request.orderId)
            .orElseThrow { CustomException(ErrorCode.ORDER_NOT_FOUND) }
        if (order.userId != userId) throw CustomException(ErrorCode.ORDER_NOT_OWNED)
        if (order.status != OrderStatus.PENDING) throw CustomException(ErrorCode.ORDER_NOT_PAYABLE)

        val existingSession = pgPaymentSessionRepository.findByOrderId(order.id)
        if (existingSession != null) pgPaymentSessionRepository.delete(existingSession)

        val (discountAmount, _) = if (request.couponId != null) {
            couponService.validateAndApplyCoupon(userId, request.couponId, order)
        } else {
            0L to null
        }

        val pointAmount = request.pointAmount ?: 0L
        if (pointAmount > 0) validatePointAmount(userId, pointAmount, order)

        val finalAmount = order.totalAmount - discountAmount - pointAmount
        val orderName = order.items.firstOrNull()?.productName ?: "상품"

        pgPaymentSessionRepository.save(
            PgPaymentSession(
                orderId = order.id,
                userId = userId,
                pgType = PaymentMethod.TOSS_PAY,
                tid = null,
                amount = finalAmount,
                discountAmount = discountAmount,
                couponId = request.couponId,
                pointAmount = pointAmount,
            )
        )

        return TossReadyResponse(
            amount = finalAmount,
            orderId = order.id,
            orderName = orderName,
        )
    }

    fun confirmTossPayment(userId: Long, request: TossConfirmRequest): PaymentResponse {
        val session = pgPaymentSessionRepository.findByOrderIdAndStatus(request.orderId, PgSessionStatus.PENDING)
            ?: throw CustomException(ErrorCode.PG_SESSION_NOT_FOUND)
        if (session.userId != userId) throw CustomException(ErrorCode.ORDER_NOT_OWNED)
        if (request.amount != session.amount) throw CustomException(ErrorCode.PG_AMOUNT_MISMATCH)

        val confirmResponse = tossPayClient.confirm(
            TossPayClient.ConfirmRequest(
                paymentKey = request.paymentKey,
                orderId = request.orderId.toString(),
                amount = request.amount,
            )
        )

        val order = orderRepository.findById(request.orderId)
            .orElseThrow { CustomException(ErrorCode.ORDER_NOT_FOUND) }
        if (order.status != OrderStatus.PENDING) throw CustomException(ErrorCode.ORDER_NOT_PAYABLE)

        if (session.pointAmount > 0) {
            pointService.validateAndUsePoints(userId, session.pointAmount, order)
        }

        val payment = paymentRepository.save(
            Payment(
                orderId = order.id,
                userId = userId,
                amount = session.amount,
                discountAmount = session.discountAmount,
                couponId = session.couponId,
                pointAmount = session.pointAmount,
                method = PaymentMethod.TOSS_PAY,
                status = PaymentStatus.COMPLETED,
                pgTransactionId = confirmResponse.paymentKey,
            )
        )

        order.status = OrderStatus.PAID

        val tossCouponId = session.couponId
        if (tossCouponId != null) {
            couponService.markAsUsed(tossCouponId, order.id)
        }
        pointService.earnPurchasePoints(userId, order.id, session.amount)
        payment.earnedPoints = session.amount / 100

        session.status = PgSessionStatus.COMPLETED

        return PaymentResponse.from(payment, order.totalAmount, payment.earnedPoints)
    }

    /**
     * initiate 단계에서 포인트 잔액/한도만 사전 검증 (차감 없음).
     * 실제 차감은 approve/confirm 단계에서 validateAndUsePoints()로 수행한다.
     */
    private fun validatePointAmount(userId: Long, pointAmount: Long, order: Order) {
        if (pointAmount < 1000) throw CustomException(ErrorCode.POINT_BELOW_MINIMUM)
        val maxUsable = order.totalAmount * 50 / 100
        if (pointAmount > maxUsable) throw CustomException(ErrorCode.POINT_EXCEEDS_MAXIMUM)
        val userPoint = pointService.getOrCreateUserPoint(userId)
        if (userPoint.balance < pointAmount) throw CustomException(ErrorCode.POINT_INSUFFICIENT)
    }
}
