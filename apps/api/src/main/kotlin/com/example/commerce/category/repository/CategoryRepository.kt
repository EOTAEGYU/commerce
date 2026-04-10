package com.example.commerce.category.repository

import com.example.commerce.category.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CategoryRepository : JpaRepository<Category, Long> {
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.displayOrder")
    fun findAllRoots(): List<Category>

    fun existsByParent(parent: Category): Boolean
}
