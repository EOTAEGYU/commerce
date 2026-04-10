package com.example.commerce.cart.repository

import com.example.commerce.cart.entity.CartItem
import org.springframework.data.jpa.repository.JpaRepository

interface CartItemRepository : JpaRepository<CartItem, Long> {
    fun findByCartIdAndProductOptionId(cartId: Long, productOptionId: Long): CartItem?
}
