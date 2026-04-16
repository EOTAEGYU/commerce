package com.example.commerce.product.repository

import com.example.commerce.product.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProductRepository : JpaRepository<Product, Long> {
    fun existsByCategoryId(categoryId: Long): Boolean

    @Query("""
        SELECT p FROM Product p
        WHERE (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND LOWER(p.name) LIKE :pattern
    """)
    fun search(categoryId: Long?, pattern: String, pageable: Pageable): Page<Product>

    fun findByIdIn(ids: List<Long>, pageable: Pageable): Page<Product>
}
