package com.example.commerce.review.dto

import com.example.commerce.review.entity.Review
import java.time.LocalDateTime

data class ReviewResponse(
    val id: Long,
    val userId: Long,
    val userName: String?,
    val productId: Long,
    val orderItemId: Long,
    val rating: Double,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(review: Review, userName: String? = null): ReviewResponse = ReviewResponse(
            id = review.id,
            userId = review.userId,
            userName = userName,
            productId = review.productId,
            orderItemId = review.orderItemId,
            rating = review.rating,
            content = review.content,
            createdAt = review.createdAt,
            updatedAt = review.updatedAt,
        )
    }
}
