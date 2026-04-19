package com.example.commerce.like.repository

import com.example.commerce.like.entity.ProductLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductLikeRepository : JpaRepository<ProductLike, Long> {
    fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLike?
    fun countByProductId(productId: Long): Long
    fun findAllByUserId(userId: Long): List<ProductLike>

    @Query("SELECT pl.productId FROM ProductLike pl WHERE pl.userId = :userId AND pl.productId IN :productIds")
    fun findLikedProductIds(@Param("userId") userId: Long, @Param("productIds") productIds: List<Long>): List<Long>
}
