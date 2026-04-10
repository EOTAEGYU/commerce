package com.example.commerce.product.repository

import com.example.commerce.product.entity.ProductOption
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface ProductOptionRepository : JpaRepository<ProductOption, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ProductOption o WHERE o.id = :id")
    fun findByIdWithLock(id: Long): ProductOption?
}
