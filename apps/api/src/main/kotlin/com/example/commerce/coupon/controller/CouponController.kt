package com.example.commerce.coupon.controller

import com.example.commerce.common.ApiResponse
import com.example.commerce.coupon.dto.UserCouponResponse
import com.example.commerce.coupon.service.CouponService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/coupons")
class CouponController(
    private val couponService: CouponService,
) {
    @PostMapping("/{templateId}/issue")
    fun issueCoupon(
        authentication: Authentication,
        @PathVariable templateId: Long,
    ): ResponseEntity<ApiResponse<UserCouponResponse>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(couponService.issueCoupon(authentication.principal as Long, templateId)))

    @GetMapping("/me")
    fun getMyCoupons(
        authentication: Authentication,
    ): ResponseEntity<ApiResponse<List<UserCouponResponse>>> =
        ResponseEntity.ok(ApiResponse.success(couponService.getMyCoupons(authentication.principal as Long)))
}
