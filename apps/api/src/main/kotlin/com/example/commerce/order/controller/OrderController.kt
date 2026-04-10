package com.example.commerce.order.controller

import com.example.commerce.common.ApiResponse
import com.example.commerce.order.dto.OrderResponse
import com.example.commerce.order.service.OrderService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping
    fun createOrder(authentication: Authentication): ResponseEntity<ApiResponse<OrderResponse>> =
        ResponseEntity.ok(ApiResponse.success(orderService.createOrder(authentication.principal as Long)))

    @GetMapping
    fun getOrders(authentication: Authentication): ResponseEntity<ApiResponse<List<OrderResponse>>> =
        ResponseEntity.ok(ApiResponse.success(orderService.getOrders(authentication.principal as Long)))

    @GetMapping("/{id}")
    fun getOrder(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<OrderResponse>> =
        ResponseEntity.ok(ApiResponse.success(orderService.getOrder(authentication.principal as Long, id)))

    @PostMapping("/{id}/cancel")
    fun cancelOrder(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<OrderResponse>> =
        ResponseEntity.ok(ApiResponse.success(orderService.cancelOrder(authentication.principal as Long, id)))
}
