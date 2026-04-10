package com.example.commerce.cart.controller

import com.example.commerce.cart.dto.CartItemAddRequest
import com.example.commerce.cart.dto.CartItemUpdateRequest
import com.example.commerce.cart.dto.CartResponse
import com.example.commerce.cart.service.CartService
import com.example.commerce.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cart")
class CartController(
    private val cartService: CartService,
) {
    @GetMapping
    fun getCart(authentication: Authentication): ResponseEntity<ApiResponse<CartResponse>> =
        ResponseEntity.ok(ApiResponse.success(cartService.getCart(authentication.principal as Long)))

    @PostMapping("/items")
    fun addItem(
        authentication: Authentication,
        @Valid @RequestBody request: CartItemAddRequest,
    ): ResponseEntity<ApiResponse<CartResponse>> =
        ResponseEntity.ok(ApiResponse.success(cartService.addItem(authentication.principal as Long, request)))

    @PutMapping("/items/{id}")
    fun updateItem(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: CartItemUpdateRequest,
    ): ResponseEntity<ApiResponse<CartResponse>> =
        ResponseEntity.ok(ApiResponse.success(cartService.updateItem(authentication.principal as Long, id, request)))

    @DeleteMapping("/items/{id}")
    fun deleteItem(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<CartResponse>> =
        ResponseEntity.ok(ApiResponse.success(cartService.deleteItem(authentication.principal as Long, id)))
}
