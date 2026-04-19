package com.example.commerce.settlement.entity

import com.example.commerce.common.BaseEntity
import com.example.commerce.payment.entity.Payment
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "settlements")
class Settlement(
    @Column(nullable = false, unique = true)
    val settlementDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SettlementStatus = SettlementStatus.PENDING,

    @Column(nullable = false)
    val orderCount: Int,

    @Column(nullable = false)
    val totalOrderAmount: Long,

    @Column(nullable = false)
    val totalDiscountAmount: Long,

    @Column(nullable = false)
    val totalPointAmount: Long,

    @Column(nullable = false)
    val totalNetAmount: Long,

    @Column(nullable = false)
    val totalEarnedPoints: Long,

    @Column
    var confirmedAt: LocalDateTime? = null,

    @Column
    var paidAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity() {
    companion object {
        fun create(date: LocalDate, payments: List<Payment>): Settlement {
            return Settlement(
                settlementDate = date,
                status = SettlementStatus.PENDING,
                orderCount = payments.size,
                totalOrderAmount = payments.sumOf { it.amount + it.discountAmount + it.pointAmount },
                totalDiscountAmount = payments.sumOf { it.discountAmount },
                totalPointAmount = payments.sumOf { it.pointAmount },
                totalNetAmount = payments.sumOf { it.amount },
                totalEarnedPoints = payments.sumOf { it.earnedPoints },
            )
        }
    }
}
