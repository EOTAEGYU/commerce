package com.example.commerce.coupon.dto

import com.example.commerce.coupon.entity.DiscountType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CouponTemplateCreateRequest(
    @field:NotBlank val name: String,
    @field:NotNull val discountType: DiscountType,
    @field:NotNull @field:Min(1) val discountValue: Long,
    val maxDiscountAmount: Long? = null,
    val minOrderAmount: Long? = null,
    val categoryId: Long? = null,
    val totalQuantity: Int? = null,
    @field:NotNull val validFrom: LocalDateTime,
    @field:NotNull val validUntil: LocalDateTime,
)
