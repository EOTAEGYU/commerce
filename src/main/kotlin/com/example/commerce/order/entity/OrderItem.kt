package com.example.commerce.order.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "order_items")
class OrderItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val productOptionId: Long,

    @Column(nullable = false)
    val productName: String,

    @Column(nullable = false)
    val optionInfo: String,

    @Column(nullable = false)
    val price: Long,

    @Column(nullable = false)
    val quantity: Int,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
