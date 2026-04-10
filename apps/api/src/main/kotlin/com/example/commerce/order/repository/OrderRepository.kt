package com.example.commerce.order.repository

import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface OrderRepository : JpaRepository<Order, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Order>
    fun findByStatusAndCreatedAtBefore(status: OrderStatus, dateTime: LocalDateTime): List<Order>
}
