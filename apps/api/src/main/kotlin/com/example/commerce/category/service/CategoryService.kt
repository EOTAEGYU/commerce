package com.example.commerce.category.service

import com.example.commerce.category.dto.CategoryCreateRequest
import com.example.commerce.category.dto.CategoryResponse
import com.example.commerce.category.dto.CategoryUpdateRequest
import com.example.commerce.category.entity.Category
import com.example.commerce.category.repository.CategoryRepository
import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CategoryService(
    private val categoryRepository: CategoryRepository,
) {
    fun create(request: CategoryCreateRequest): CategoryResponse {
        val parent = request.parentId?.let {
            categoryRepository.findById(it)
                .orElseThrow { CustomException(ErrorCode.CATEGORY_NOT_FOUND) }
        }
        val category = Category(
            name = request.name,
            parent = parent,
            displayOrder = request.displayOrder,
        )
        return CategoryResponse.from(categoryRepository.save(category))
    }

    @Transactional(readOnly = true)
    fun getTree(): List<CategoryResponse> =
        categoryRepository.findAllRoots().map { CategoryResponse.from(it) }

    fun update(id: Long, request: CategoryUpdateRequest): CategoryResponse {
        val category = categoryRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.CATEGORY_NOT_FOUND) }
        category.name = request.name
        category.displayOrder = request.displayOrder
        return CategoryResponse.from(category)
    }

    fun delete(id: Long) {
        val category = categoryRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.CATEGORY_NOT_FOUND) }
        if (categoryRepository.existsByParent(category))
            throw CustomException(ErrorCode.CATEGORY_HAS_CHILDREN)
        categoryRepository.delete(category)
    }
}
