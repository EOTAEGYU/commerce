package com.example.commerce.payment.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "pg_payment_sessions")
class PgPaymentSession(
    @Column(nullable = false, unique = true)
    val orderId: Long,

    @Column(nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val pgType: PaymentMethod,

    @Column
    val tid: String? = null,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    val discountAmount: Long = 0,

    @Column
    val couponId: Long? = null,

    @Column(nullable = false)
    val pointAmount: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PgSessionStatus = PgSessionStatus.PENDING,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
