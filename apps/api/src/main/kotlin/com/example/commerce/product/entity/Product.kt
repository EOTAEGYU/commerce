package com.example.commerce.product.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "products")
class Product(
    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false)
    var price: Long,

    @Column(nullable = false)
    var categoryId: Long,

    @Column
    var imageUrl: String? = null,

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    val options: MutableList<ProductOption> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
