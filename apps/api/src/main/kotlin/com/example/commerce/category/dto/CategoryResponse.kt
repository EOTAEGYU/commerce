package com.example.commerce.category.dto

import com.example.commerce.category.entity.Category

data class CategoryResponse(
    val id: Long,
    val name: String,
    val displayOrder: Int,
    val children: List<CategoryResponse>,
) {
    companion object {
        fun from(category: Category): CategoryResponse = CategoryResponse(
            id = category.id,
            name = category.name,
            displayOrder = category.displayOrder,
            children = category.children
                .sortedBy { it.displayOrder }
                .map { from(it) },
        )
    }
}
