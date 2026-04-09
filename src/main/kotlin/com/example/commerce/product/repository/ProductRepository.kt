package com.example.commerce.product.repository

import com.example.commerce.product.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long> {
    fun findByCategoryId(categoryId: Long, pageable: Pageable): Page<Product>
    fun existsByCategoryId(categoryId: Long): Boolean
}
