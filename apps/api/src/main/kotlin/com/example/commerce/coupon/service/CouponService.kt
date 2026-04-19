package com.example.commerce.coupon.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.coupon.dto.CouponTemplateCreateRequest
import com.example.commerce.coupon.dto.CouponTemplateResponse
import com.example.commerce.coupon.dto.UserCouponResponse
import com.example.commerce.coupon.entity.CouponTemplate
import com.example.commerce.coupon.entity.UserCoupon
import com.example.commerce.coupon.entity.UserCouponStatus
import com.example.commerce.coupon.repository.CouponTemplateRepository
import com.example.commerce.coupon.repository.UserCouponRepository
import com.example.commerce.order.entity.Order
import com.example.commerce.product.repository.ProductRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class CouponService(
    private val couponTemplateRepository: CouponTemplateRepository,
    private val userCouponRepository: UserCouponRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun issueCoupon(userId: Long, templateId: Long): UserCouponResponse {
        val template = couponTemplateRepository.findById(templateId)
            .orElseThrow { CustomException(ErrorCode.COUPON_NOT_FOUND) }

        if (!template.isActive) throw CustomException(ErrorCode.COUPON_NOT_ACTIVE)

        val now = LocalDateTime.now()
        if (now.isBefore(template.validFrom) || now.isAfter(template.validUntil)) {
            throw CustomException(ErrorCode.COUPON_EXPIRED)
        }

        if (userCouponRepository.findByUserIdAndCouponTemplateId(userId, templateId) != null) {
            throw CustomException(ErrorCode.COUPON_ALREADY_ISSUED)
        }

        val totalQuantity = template.totalQuantity
        if (totalQuantity != null && template.issuedCount >= totalQuantity) {
            throw CustomException(ErrorCode.COUPON_QUANTITY_EXHAUSTED)
        }

        try {
            template.issuedCount++
            couponTemplateRepository.save(template)
        } catch (e: ObjectOptimisticLockingFailureException) {
            throw CustomException(ErrorCode.COUPON_QUANTITY_EXHAUSTED)
        }

        val userCoupon = try {
            userCouponRepository.save(
                UserCoupon(
                    userId = userId,
                    couponTemplateId = templateId,
                )
            )
        } catch (e: DataIntegrityViolationException) {
            throw CustomException(ErrorCode.COUPON_ALREADY_ISSUED)
        }

        return UserCouponResponse.from(userCoupon, template)
    }

    fun getMyCoupons(userId: Long): List<UserCouponResponse> {
        val userCoupons = userCouponRepository.findAllByUserId(userId)
        val templateIds = userCoupons.map { it.couponTemplateId }
        val templates = couponTemplateRepository.findAllById(templateIds).associateBy { it.id }
        return userCoupons.map { userCoupon ->
            val template = templates[userCoupon.couponTemplateId]
                ?: throw CustomException(ErrorCode.COUPON_NOT_FOUND)
            UserCouponResponse.from(userCoupon, template)
        }
    }

    fun validateAndApplyCoupon(userId: Long, userCouponId: Long, order: Order): Pair<Long, Long> {
        val userCoupon = userCouponRepository.findById(userCouponId)
            .orElseThrow { CustomException(ErrorCode.COUPON_NOT_FOUND) }

        if (userCoupon.userId != userId) throw CustomException(ErrorCode.COUPON_NOT_OWNED)
        if (userCoupon.status != UserCouponStatus.UNUSED) throw CustomException(ErrorCode.COUPON_ALREADY_USED)

        val now = LocalDateTime.now()
        val template = couponTemplateRepository.findById(userCoupon.couponTemplateId)
            .orElseThrow { CustomException(ErrorCode.COUPON_NOT_FOUND) }

        if (now.isBefore(template.validFrom) || now.isAfter(template.validUntil)) {
            throw CustomException(ErrorCode.COUPON_EXPIRED)
        }

        val minOrderAmount = template.minOrderAmount
        if (minOrderAmount != null && order.totalAmount < minOrderAmount) {
            throw CustomException(ErrorCode.COUPON_MIN_AMOUNT_NOT_MET)
        }

        if (template.categoryId != null) {
            val productIds = order.items.map { it.productId }
            val products = productRepository.findAllById(productIds)
            val hasMatchingCategory = products.any { it.categoryId == template.categoryId }
            if (!hasMatchingCategory) throw CustomException(ErrorCode.COUPON_CATEGORY_NOT_MET)
        }

        val discountAmount = when (template.discountType) {
            com.example.commerce.coupon.entity.DiscountType.FIXED ->
                minOf(template.discountValue, order.totalAmount)
            com.example.commerce.coupon.entity.DiscountType.RATE -> {
                val calculated = order.totalAmount * template.discountValue / 100
                val maxDiscountAmount = template.maxDiscountAmount
                if (maxDiscountAmount != null) minOf(calculated, maxDiscountAmount)
                else calculated
            }
        }

        return discountAmount to userCouponId
    }

    @Transactional
    fun markAsUsed(userCouponId: Long, orderId: Long) {
        val userCoupon = userCouponRepository.findById(userCouponId)
            .orElseThrow { CustomException(ErrorCode.COUPON_NOT_FOUND) }
        userCoupon.status = UserCouponStatus.USED
        userCoupon.usedOrderId = orderId
        userCoupon.usedAt = LocalDateTime.now()
    }

    @Transactional
    fun createCouponTemplate(request: CouponTemplateCreateRequest): CouponTemplateResponse {
        val template = couponTemplateRepository.save(
            CouponTemplate(
                name = request.name,
                discountType = request.discountType,
                discountValue = request.discountValue,
                maxDiscountAmount = request.maxDiscountAmount,
                minOrderAmount = request.minOrderAmount,
                categoryId = request.categoryId,
                totalQuantity = request.totalQuantity,
                validFrom = request.validFrom,
                validUntil = request.validUntil,
            )
        )
        return CouponTemplateResponse.from(template)
    }

    fun getAllCouponTemplates(): List<CouponTemplateResponse> =
        couponTemplateRepository.findAll().map { CouponTemplateResponse.from(it) }

    @Transactional
    fun toggleCouponTemplate(id: Long): CouponTemplateResponse {
        val template = couponTemplateRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.COUPON_NOT_FOUND) }
        template.isActive = !template.isActive
        return CouponTemplateResponse.from(template)
    }
}
