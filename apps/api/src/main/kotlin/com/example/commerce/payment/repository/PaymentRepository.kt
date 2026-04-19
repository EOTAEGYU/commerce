package com.example.commerce.payment.repository

import com.example.commerce.payment.entity.Payment
import com.example.commerce.payment.entity.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByOrderId(orderId: Long): Payment?
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Payment>
    fun findAllByStatusAndCreatedAtBetween(
        status: PaymentStatus,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<Payment>
}
