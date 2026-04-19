package com.example.commerce.review.controller

import com.example.commerce.common.ApiResponse
import com.example.commerce.review.dto.ReviewCreateRequest
import com.example.commerce.review.dto.ReviewResponse
import com.example.commerce.review.dto.ReviewUpdateRequest
import com.example.commerce.review.service.ReviewService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
class ReviewController(
    private val reviewService: ReviewService,
) {
    @PostMapping("/api/reviews")
    fun createReview(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: ReviewCreateRequest,
    ): ResponseEntity<ApiResponse<ReviewResponse>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(reviewService.createReview(userId, request)))

    @PutMapping("/api/reviews/{id}")
    fun updateReview(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @RequestBody @Valid request: ReviewUpdateRequest,
    ): ResponseEntity<ApiResponse<ReviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(reviewService.updateReview(userId, id, request)))

    @DeleteMapping("/api/reviews/{id}")
    fun deleteReview(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        reviewService.deleteReview(userId, id)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    @GetMapping("/api/products/{productId}/reviews")
    fun getProductReviews(
        @PathVariable productId: Long,
        @PageableDefault(size = 10) pageable: Pageable,
    ): ResponseEntity<ApiResponse<Page<ReviewResponse>>> =
        ResponseEntity.ok(ApiResponse.success(reviewService.getProductReviews(productId, pageable)))

    @GetMapping("/api/reviews/my")
    fun getMyReviews(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<ApiResponse<List<ReviewResponse>>> =
        ResponseEntity.ok(ApiResponse.success(reviewService.getMyReviews(userId)))
}
