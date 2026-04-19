package com.example.commerce.coupon.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDateTime

@Entity
@Table(name = "coupon_templates")
class CouponTemplate(
    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val discountType: DiscountType,

    @Column(nullable = false)
    val discountValue: Long,

    @Column
    val maxDiscountAmount: Long? = null,

    @Column
    val minOrderAmount: Long? = null,

    @Column
    val categoryId: Long? = null,

    @Column
    val totalQuantity: Int? = null,

    @Column(nullable = false)
    var issuedCount: Int = 0,

    @Column(nullable = false)
    val validFrom: LocalDateTime,

    @Column(nullable = false)
    val validUntil: LocalDateTime,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Version
    val version: Long = 0,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
