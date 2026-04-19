package com.example.commerce.order.dto

import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderItem
import com.example.commerce.order.entity.OrderStatus

data class OrderResponse(
    val id: Long,
    val userId: Long,
    val status: OrderStatus,
    val totalAmount: Long,
    val items: List<OrderItemResponse>,
    val createdAt: String,
) {
    companion object {
        fun from(order: Order): OrderResponse = OrderResponse(
            id = order.id,
            userId = order.userId,
            status = order.status,
            totalAmount = order.totalAmount,
            items = order.items.map { OrderItemResponse.from(it) },
            createdAt = order.createdAt.toString(),
        )
    }
}

data class OrderItemResponse(
    val id: Long,
    val productId: Long,
    val productOptionId: Long,
    val productName: String,
    val optionInfo: String,
    val price: Long,
    val quantity: Int,
    val totalPrice: Long,
) {
    companion object {
        fun from(item: OrderItem): OrderItemResponse = OrderItemResponse(
            id = item.id,
            productId = item.productId,
            productOptionId = item.productOptionId,
            productName = item.productName,
            optionInfo = item.optionInfo,
            price = item.price,
            quantity = item.quantity,
            totalPrice = item.price * item.quantity,
        )
    }
}
