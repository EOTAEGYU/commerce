package com.example.commerce.coupon.dto

import com.example.commerce.coupon.entity.CouponTemplate
import com.example.commerce.coupon.entity.DiscountType
import com.example.commerce.coupon.entity.UserCoupon
import com.example.commerce.coupon.entity.UserCouponStatus
import java.time.LocalDateTime

data class UserCouponResponse(
    val id: Long,
    val couponTemplateId: Long,
    val templateName: String,
    val discountType: DiscountType,
    val discountValue: Long,
    val maxDiscountAmount: Long?,
    val minOrderAmount: Long?,
    val categoryId: Long?,
    val status: UserCouponStatus,
    val usedOrderId: Long?,
    val usedAt: LocalDateTime?,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
) {
    companion object {
        fun from(userCoupon: UserCoupon, template: CouponTemplate): UserCouponResponse = UserCouponResponse(
            id = userCoupon.id,
            couponTemplateId = template.id,
            templateName = template.name,
            discountType = template.discountType,
            discountValue = template.discountValue,
            maxDiscountAmount = template.maxDiscountAmount,
            minOrderAmount = template.minOrderAmount,
            categoryId = template.categoryId,
            status = userCoupon.status,
            usedOrderId = userCoupon.usedOrderId,
            usedAt = userCoupon.usedAt,
            validFrom = template.validFrom,
            validUntil = template.validUntil,
        )
    }
}
