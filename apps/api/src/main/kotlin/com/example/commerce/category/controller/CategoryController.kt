package com.example.commerce.category.controller

import com.example.commerce.category.dto.CategoryCreateRequest
import com.example.commerce.category.dto.CategoryResponse
import com.example.commerce.category.dto.CategoryUpdateRequest
import com.example.commerce.category.service.CategoryService
import com.example.commerce.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService,
) {
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun create(@Valid @RequestBody request: CategoryCreateRequest): ResponseEntity<ApiResponse<CategoryResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(categoryService.create(request)))

    @GetMapping
    fun getTree(): ResponseEntity<ApiResponse<List<CategoryResponse>>> =
        ResponseEntity.ok(ApiResponse.success(categoryService.getTree()))

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: CategoryUpdateRequest,
    ): ResponseEntity<ApiResponse<CategoryResponse>> =
        ResponseEntity.ok(ApiResponse.success(categoryService.update(id, request)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        categoryService.delete(id)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }
}
