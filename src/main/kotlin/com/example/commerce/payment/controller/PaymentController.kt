package com.example.commerce.payment.controller

import com.example.commerce.common.ApiResponse
import com.example.commerce.payment.dto.PaymentRequest
import com.example.commerce.payment.dto.PaymentResponse
import com.example.commerce.payment.service.PaymentService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val paymentService: PaymentService,
) {
    @PostMapping
    fun requestPayment(
        authentication: Authentication,
        @Valid @RequestBody request: PaymentRequest,
    ): ResponseEntity<ApiResponse<PaymentResponse>> =
        ResponseEntity.ok(
            ApiResponse.success(paymentService.requestPayment(authentication.principal as Long, request))
        )

    @GetMapping("/{orderId}")
    fun getPayment(
        authentication: Authentication,
        @PathVariable orderId: Long,
    ): ResponseEntity<ApiResponse<PaymentResponse>> =
        ResponseEntity.ok(
            ApiResponse.success(paymentService.getPayment(authentication.principal as Long, orderId))
        )
}
