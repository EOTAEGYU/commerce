package com.example.commerce.payment.dto

import com.example.commerce.payment.entity.Payment
import com.example.commerce.payment.entity.PaymentMethod
import com.example.commerce.payment.entity.PaymentStatus

data class PaymentResponse(
    val id: Long,
    val orderId: Long,
    val userId: Long,
    val amount: Long,
    val discountAmount: Long,
    val originalAmount: Long,
    val couponId: Long?,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val pgTransactionId: String?,
    val createdAt: String,
) {
    companion object {
        fun from(payment: Payment, originalAmount: Long): PaymentResponse = PaymentResponse(
            id = payment.id,
            orderId = payment.orderId,
            userId = payment.userId,
            amount = payment.amount,
            discountAmount = payment.discountAmount,
            originalAmount = originalAmount,
            couponId = payment.couponId,
            method = payment.method,
            status = payment.status,
            pgTransactionId = payment.pgTransactionId,
            createdAt = payment.createdAt.toString(),
        )
    }
}
