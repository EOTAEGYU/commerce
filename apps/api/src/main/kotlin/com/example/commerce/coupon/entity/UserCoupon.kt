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
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_coupons",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "coupon_template_id"])],
)
class UserCoupon(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "coupon_template_id", nullable = false)
    val couponTemplateId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserCouponStatus = UserCouponStatus.UNUSED,

    @Column
    var usedOrderId: Long? = null,

    @Column
    var usedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
