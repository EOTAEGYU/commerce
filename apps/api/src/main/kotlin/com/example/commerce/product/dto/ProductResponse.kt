package com.example.commerce.product.dto

import com.example.commerce.product.entity.Product
import com.example.commerce.product.entity.ProductOption

data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val categoryId: Long,
    val imageUrl: String?,
    val options: List<ProductOptionResponse>,
    val averageRating: Double? = null,
    val reviewCount: Long = 0L,
) {
    companion object {
        fun from(
            product: Product,
            averageRating: Double? = null,
            reviewCount: Long = 0L,
        ): ProductResponse = ProductResponse(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            categoryId = product.categoryId,
            imageUrl = product.imageUrl,
            options = product.options.map { ProductOptionResponse.from(it) },
            averageRating = averageRating,
            reviewCount = reviewCount,
        )
    }
}

data class ProductOptionResponse(
    val id: Long,
    val size: String,
    val color: String,
    val stock: Int,
) {
    companion object {
        fun from(option: ProductOption): ProductOptionResponse = ProductOptionResponse(
            id = option.id,
            size = option.size,
            color = option.color,
            stock = option.stock,
        )
    }
}
