package com.example.commerce.review.repository

import com.example.commerce.review.entity.Review
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ReviewRepository : JpaRepository<Review, Long> {
    fun existsByOrderItemId(orderItemId: Long): Boolean
    fun findAllByProductIdOrderByCreatedAtDesc(productId: Long, pageable: Pageable): Page<Review>
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Review>

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    fun findAverageRatingByProductId(productId: Long): Double?

    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId")
    fun countByProductId(productId: Long): Long

    @Query("SELECT r.productId, AVG(r.rating), COUNT(r) FROM Review r WHERE r.productId IN :productIds GROUP BY r.productId")
    fun findStatsByProductIds(productIds: List<Long>): List<Array<Any>>
}
