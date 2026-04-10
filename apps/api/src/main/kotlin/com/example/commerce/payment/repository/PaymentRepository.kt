package com.example.commerce.payment.repository

import com.example.commerce.payment.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByOrderId(orderId: Long): Payment?
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Payment>
}
