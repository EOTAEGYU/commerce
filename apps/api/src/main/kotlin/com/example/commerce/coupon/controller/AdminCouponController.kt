package com.example.commerce.coupon.controller

import com.example.commerce.common.ApiResponse
import com.example.commerce.coupon.dto.CouponTemplateCreateRequest
import com.example.commerce.coupon.dto.CouponTemplateResponse
import com.example.commerce.coupon.service.CouponService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/coupons")
class AdminCouponController(
    private val couponService: CouponService,
) {
    @PostMapping
    fun createCouponTemplate(
        @Valid @RequestBody request: CouponTemplateCreateRequest,
    ): ResponseEntity<ApiResponse<CouponTemplateResponse>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(couponService.createCouponTemplate(request)))

    @GetMapping
    fun getAllCouponTemplates(): ResponseEntity<ApiResponse<List<CouponTemplateResponse>>> =
        ResponseEntity.ok(ApiResponse.success(couponService.getAllCouponTemplates()))

    @PatchMapping("/{id}/toggle")
    fun toggleCouponTemplate(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<CouponTemplateResponse>> =
        ResponseEntity.ok(ApiResponse.success(couponService.toggleCouponTemplate(id)))
}
