package com.example.commerce.product.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "product_options")
class ProductOption(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(nullable = false)
    var size: String,

    @Column(nullable = false)
    var color: String,

    @Column(nullable = false)
    var stock: Int,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
