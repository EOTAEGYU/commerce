package com.example.commerce.cart.repository

import com.example.commerce.cart.entity.Cart
import org.springframework.data.jpa.repository.JpaRepository

interface CartRepository : JpaRepository<Cart, Long> {
    fun findByUserId(userId: Long): Cart?
}
