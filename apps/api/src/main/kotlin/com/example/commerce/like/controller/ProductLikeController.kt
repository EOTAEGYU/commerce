package com.example.commerce.like.controller

import com.example.commerce.common.ApiResponse
import com.example.commerce.like.dto.LikeResponse
import com.example.commerce.like.service.ProductLikeService
import com.example.commerce.product.dto.ProductResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/likes")
class ProductLikeController(
    private val productLikeService: ProductLikeService,
) {
    @PostMapping("/{productId}")
    fun toggle(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
    ): ResponseEntity<ApiResponse<LikeResponse>> =
        ResponseEntity.ok(ApiResponse.success(productLikeService.toggle(userId, productId)))

    @GetMapping("/my")
    fun getMyLikedProducts(
        @AuthenticationPrincipal userId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<Page<ProductResponse>>> =
        ResponseEntity.ok(ApiResponse.success(productLikeService.getMyLikedProducts(userId, pageable)))

    @GetMapping("/status")
    fun getLikeStatus(
        @AuthenticationPrincipal userId: Long,
        @RequestParam productIds: List<Long>,
    ): ResponseEntity<ApiResponse<Map<Long, Boolean>>> =
        ResponseEntity.ok(ApiResponse.success(productLikeService.getLikeStatus(userId, productIds)))
}
