package com.example.commerce.coupon.repository

import com.example.commerce.coupon.entity.CouponTemplate
import org.springframework.data.jpa.repository.JpaRepository

interface CouponTemplateRepository : JpaRepository<CouponTemplate, Long> {
    fun findAllByIsActiveTrue(): List<CouponTemplate>
}
