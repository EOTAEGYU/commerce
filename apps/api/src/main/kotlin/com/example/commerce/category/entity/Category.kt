package com.example.commerce.category.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "categories")
class Category(
    @Column(nullable = false)
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Category? = null,

    @Column(nullable = false)
    var displayOrder: Int = 0,

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL])
    val children: MutableList<Category> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
