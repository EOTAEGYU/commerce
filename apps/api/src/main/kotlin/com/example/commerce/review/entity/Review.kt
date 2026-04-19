package com.example.commerce.review.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = [UniqueConstraint(columnNames = ["order_item_id"])],
)
class Review(
    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val orderId: Long,

    @Column(name = "order_item_id", nullable = false, unique = true)
    val orderItemId: Long,

    @Column(nullable = false)
    var rating: Double,

    @Column(nullable = false, length = 500)
    var content: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
