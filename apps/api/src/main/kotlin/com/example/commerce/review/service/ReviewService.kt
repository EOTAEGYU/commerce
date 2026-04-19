package com.example.commerce.review.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.order.repository.OrderItemRepository
import com.example.commerce.order.repository.OrderRepository
import com.example.commerce.point.service.PointService
import com.example.commerce.review.dto.ReviewCreateRequest
import com.example.commerce.review.dto.ReviewResponse
import com.example.commerce.review.dto.ReviewUpdateRequest
import com.example.commerce.review.entity.Review
import com.example.commerce.review.repository.ReviewRepository
import com.example.commerce.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val pointService: PointService,
) {
    @Transactional
    fun createReview(userId: Long, request: ReviewCreateRequest): ReviewResponse {
        val orderItem = orderItemRepository.findById(request.orderItemId)
            .orElseThrow { CustomException(ErrorCode.ORDER_ITEM_NOT_FOUND) }

        val order = orderRepository.findById(orderItem.order.id)
            .orElseThrow { CustomException(ErrorCode.ORDER_NOT_FOUND) }

        if (order.userId != userId) throw CustomException(ErrorCode.ORDER_NOT_OWNED)

        if (order.status != OrderStatus.DELIVERED) throw CustomException(ErrorCode.ORDER_NOT_DELIVERED)

        if (reviewRepository.existsByOrderItemId(request.orderItemId)) throw CustomException(ErrorCode.REVIEW_ALREADY_EXISTS)

        if ((request.rating * 2) % 1 != 0.0) throw CustomException(ErrorCode.INVALID_RATING)

        val review = Review(
            userId = userId,
            productId = orderItem.productId,
            orderId = order.id,
            orderItemId = orderItem.id,
            rating = request.rating,
            content = request.content,
        )
        val saved = reviewRepository.save(review)
        pointService.earnReviewPoints(userId, saved.id)
        val userName = userRepository.findById(userId).map { it.name }.orElse(null)
        return ReviewResponse.from(saved, userName)
    }

    @Transactional
    fun updateReview(userId: Long, reviewId: Long, request: ReviewUpdateRequest): ReviewResponse {
        val review = reviewRepository.findById(reviewId)
            .orElseThrow { CustomException(ErrorCode.REVIEW_NOT_FOUND) }

        if (review.userId != userId) throw CustomException(ErrorCode.REVIEW_NOT_OWNED)

        if ((request.rating * 2) % 1 != 0.0) throw CustomException(ErrorCode.INVALID_RATING)

        review.content = request.content
        review.rating = request.rating

        val userName = userRepository.findById(userId).map { it.name }.orElse(null)
        return ReviewResponse.from(review, userName)
    }

    @Transactional
    fun deleteReview(userId: Long, reviewId: Long) {
        val review = reviewRepository.findById(reviewId)
            .orElseThrow { CustomException(ErrorCode.REVIEW_NOT_FOUND) }

        if (review.userId != userId) throw CustomException(ErrorCode.REVIEW_NOT_OWNED)

        reviewRepository.delete(review)
    }

    fun getProductReviews(productId: Long, pageable: Pageable): Page<ReviewResponse> {
        return reviewRepository.findAllByProductIdOrderByCreatedAtDesc(productId, pageable)
            .map { ReviewResponse.from(it) }
    }

    fun getMyReviews(userId: Long): List<ReviewResponse> {
        val userName = userRepository.findById(userId).map { it.name }.orElse(null)
        return reviewRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
            .map { ReviewResponse.from(it, userName) }
    }
}
