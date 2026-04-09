package com.example.commerce.product.service

import com.example.commerce.category.repository.CategoryRepository
import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.product.dto.ProductCreateRequest
import com.example.commerce.product.dto.ProductResponse
import com.example.commerce.product.dto.ProductUpdateRequest
import com.example.commerce.product.entity.Product
import com.example.commerce.product.entity.ProductOption
import com.example.commerce.product.repository.ProductOptionRepository
import com.example.commerce.product.repository.ProductRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProductService(
    private val productRepository: ProductRepository,
    private val productOptionRepository: ProductOptionRepository,
    private val categoryRepository: CategoryRepository,
) {
    fun create(request: ProductCreateRequest): ProductResponse {
        if (!categoryRepository.existsById(request.categoryId))
            throw CustomException(ErrorCode.CATEGORY_NOT_FOUND)

        val product = Product(
            name = request.name,
            description = request.description,
            price = request.price,
            categoryId = request.categoryId,
        )
        request.options.forEach { opt ->
            product.options.add(ProductOption(product = product, size = opt.size, color = opt.color, stock = opt.stock))
        }
        return ProductResponse.from(productRepository.save(product))
    }

    @Transactional(readOnly = true)
    fun getList(categoryId: Long?, pageable: Pageable): Page<ProductResponse> =
        if (categoryId != null)
            productRepository.findByCategoryId(categoryId, pageable).map { ProductResponse.from(it) }
        else
            productRepository.findAll(pageable).map { ProductResponse.from(it) }

    @Transactional(readOnly = true)
    fun getOne(id: Long): ProductResponse {
        val product = productRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.PRODUCT_NOT_FOUND) }
        return ProductResponse.from(product)
    }

    fun update(id: Long, request: ProductUpdateRequest): ProductResponse {
        val product = productRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.PRODUCT_NOT_FOUND) }
        if (!categoryRepository.existsById(request.categoryId))
            throw CustomException(ErrorCode.CATEGORY_NOT_FOUND)

        product.name = request.name
        product.description = request.description
        product.price = request.price
        product.categoryId = request.categoryId
        return ProductResponse.from(product)
    }

    fun delete(id: Long) {
        if (!productRepository.existsById(id))
            throw CustomException(ErrorCode.PRODUCT_NOT_FOUND)
        productRepository.deleteById(id)
    }

    fun decreaseStock(optionId: Long, quantity: Int) {
        val option = productOptionRepository.findByIdWithLock(optionId)
            ?: throw CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND)
        if (option.stock < quantity)
            throw CustomException(ErrorCode.OUT_OF_STOCK)
        option.stock -= quantity
    }

    fun increaseStock(optionId: Long, quantity: Int) {
        val option = productOptionRepository.findByIdWithLock(optionId)
            ?: throw CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND)
        option.stock += quantity
    }
}
