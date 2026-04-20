package com.example.commerce.order.repository

import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface OrderRepository : JpaRepository<Order, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Order>
    fun findByStatusAndCreatedAtBefore(status: OrderStatus, dateTime: LocalDateTime): List<Order>

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items WHERE o.status = :status AND o.createdAt < :dateTime")
    fun findExpiredOrdersWithItems(
        @Param("status") status: OrderStatus,
        @Param("dateTime") dateTime: LocalDateTime,
    ): List<Order>
}
