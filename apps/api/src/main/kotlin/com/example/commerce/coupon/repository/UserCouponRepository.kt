package com.example.commerce.coupon.repository

import com.example.commerce.coupon.entity.UserCoupon
import org.springframework.data.jpa.repository.JpaRepository

interface UserCouponRepository : JpaRepository<UserCoupon, Long> {
    fun findByUserIdAndCouponTemplateId(userId: Long, couponTemplateId: Long): UserCoupon?
    fun findAllByUserId(userId: Long): List<UserCoupon>
}
