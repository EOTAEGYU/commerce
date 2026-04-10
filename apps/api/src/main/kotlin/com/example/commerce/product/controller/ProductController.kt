package com.example.commerce.product.controller

import com.example.commerce.common.ApiResponse
import com.example.commerce.product.dto.ProductCreateRequest
import com.example.commerce.product.dto.ProductResponse
import com.example.commerce.product.dto.ProductUpdateRequest
import com.example.commerce.product.service.ProductService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
) {
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun create(@Valid @RequestBody request: ProductCreateRequest): ResponseEntity<ApiResponse<ProductResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(productService.create(request)))

    @GetMapping
    fun getList(
        @RequestParam(required = false) categoryId: Long?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<Page<ProductResponse>>> =
        ResponseEntity.ok(ApiResponse.success(productService.getList(categoryId, pageable)))

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long): ResponseEntity<ApiResponse<ProductResponse>> =
        ResponseEntity.ok(ApiResponse.success(productService.getOne(id)))

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductUpdateRequest,
    ): ResponseEntity<ApiResponse<ProductResponse>> =
        ResponseEntity.ok(ApiResponse.success(productService.update(id, request)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        productService.delete(id)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }
}
