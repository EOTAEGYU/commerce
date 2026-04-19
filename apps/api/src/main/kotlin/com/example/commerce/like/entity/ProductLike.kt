package com.example.commerce.like.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "product_likes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "product_id"])],
    indexes = [
        Index(name = "idx_product_likes_user_id", columnList = "user_id"),
        Index(name = "idx_product_likes_product_id", columnList = "product_id"),
    ]
)
class ProductLike(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
