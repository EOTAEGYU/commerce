package com.example.commerce.cart.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "cart_items")
class CartItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val cart: Cart,

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val productOptionId: Long,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    val price: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
