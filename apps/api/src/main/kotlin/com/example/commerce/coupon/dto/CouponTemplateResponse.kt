package com.example.commerce.coupon.dto

import com.example.commerce.coupon.entity.CouponTemplate
import com.example.commerce.coupon.entity.DiscountType
import java.time.LocalDateTime

data class CouponTemplateResponse(
    val id: Long,
    val name: String,
    val discountType: DiscountType,
    val discountValue: Long,
    val maxDiscountAmount: Long?,
    val minOrderAmount: Long?,
    val categoryId: Long?,
    val totalQuantity: Int?,
    val issuedCount: Int,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(entity: CouponTemplate): CouponTemplateResponse = CouponTemplateResponse(
            id = entity.id,
            name = entity.name,
            discountType = entity.discountType,
            discountValue = entity.discountValue,
            maxDiscountAmount = entity.maxDiscountAmount,
            minOrderAmount = entity.minOrderAmount,
            categoryId = entity.categoryId,
            totalQuantity = entity.totalQuantity,
            issuedCount = entity.issuedCount,
            validFrom = entity.validFrom,
            validUntil = entity.validUntil,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
        )
    }
}
