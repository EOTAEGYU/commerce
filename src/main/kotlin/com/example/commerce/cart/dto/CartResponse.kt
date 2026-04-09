package com.example.commerce.cart.dto

import com.example.commerce.cart.entity.Cart
import com.example.commerce.cart.entity.CartItem

data class CartResponse(
    val id: Long,
    val userId: Long,
    val items: List<CartItemResponse>,
    val totalAmount: Long,
) {
    companion object {
        fun from(cart: Cart): CartResponse {
            val items = cart.items.map { CartItemResponse.from(it) }
            return CartResponse(
                id = cart.id,
                userId = cart.userId,
                items = items,
                totalAmount = items.sumOf { it.totalPrice },
            )
        }
    }
}

data class CartItemResponse(
    val id: Long,
    val productId: Long,
    val productOptionId: Long,
    val quantity: Int,
    val price: Long,
    val totalPrice: Long,
) {
    companion object {
        fun from(item: CartItem): CartItemResponse = CartItemResponse(
            id = item.id,
            productId = item.productId,
            productOptionId = item.productOptionId,
            quantity = item.quantity,
            price = item.price,
            totalPrice = item.price * item.quantity,
        )
    }
}
