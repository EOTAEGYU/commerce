package com.example.commerce.point.entity

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
@Table(name = "point_histories")
class PointHistory(
    @Column(nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: PointHistoryType,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    val balance: Long,

    @Column
    val relatedId: Long? = null,

    @Column
    val description: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity() {
    companion object {
        fun of(
            userId: Long,
            type: PointHistoryType,
            amount: Long,
            balance: Long,
            relatedId: Long? = null,
            description: String? = null,
        ): PointHistory = PointHistory(
            userId = userId,
            type = type,
            amount = amount,
            balance = balance,
            relatedId = relatedId,
            description = description,
        )
    }
}
