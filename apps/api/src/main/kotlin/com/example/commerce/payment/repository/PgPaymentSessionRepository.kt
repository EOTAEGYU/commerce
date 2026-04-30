package com.example.commerce.payment.repository

import com.example.commerce.payment.entity.PgPaymentSession
import com.example.commerce.payment.entity.PgSessionStatus
import org.springframework.data.jpa.repository.JpaRepository

interface PgPaymentSessionRepository : JpaRepository<PgPaymentSession, Long> {
    fun findByOrderId(orderId: Long): PgPaymentSession?
    fun findByOrderIdAndStatus(orderId: Long, status: PgSessionStatus): PgPaymentSession?
}
