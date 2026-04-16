package com.example.commerce.like.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.like.dto.LikeResponse
import com.example.commerce.like.entity.ProductLike
import com.example.commerce.like.repository.ProductLikeRepository
import com.example.commerce.product.dto.ProductResponse
import com.example.commerce.product.repository.ProductRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductLikeService(
    private val productLikeRepository: ProductLikeRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun toggle(userId: Long, productId: Long): LikeResponse {
        productRepository.findById(productId)
            .orElseThrow { CustomException(ErrorCode.PRODUCT_NOT_FOUND) }

        val existing = productLikeRepository.findByUserIdAndProductId(userId, productId)
        val liked: Boolean
        if (existing != null) {
            productLikeRepository.delete(existing)
            liked = false
        } else {
            productLikeRepository.save(ProductLike(userId = userId, productId = productId))
            liked = true
        }

        val likeCount = productLikeRepository.countByProductId(productId)
        return LikeResponse.of(productId, liked, likeCount)
    }

    fun getMyLikedProducts(userId: Long, pageable: Pageable): Page<ProductResponse> {
        val ids = productLikeRepository.findAllByUserId(userId).map { it.productId }
        if (ids.isEmpty()) return Page.empty(pageable)
        return productRepository.findByIdIn(ids, pageable).map { ProductResponse.from(it) }
    }

    fun getLikeStatus(userId: Long, productIds: List<Long>): Map<Long, Boolean> {
        val likedSet = productLikeRepository.findLikedProductIds(userId, productIds).toSet()
        return productIds.associateWith { it in likedSet }
    }
}
