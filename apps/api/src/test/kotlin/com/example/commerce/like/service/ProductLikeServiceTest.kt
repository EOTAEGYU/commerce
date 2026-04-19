package com.example.commerce.like.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.like.entity.ProductLike
import com.example.commerce.like.repository.ProductLikeRepository
import com.example.commerce.product.entity.Product
import com.example.commerce.product.repository.ProductRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

@ExtendWith(MockKExtension::class)
class ProductLikeServiceTest {

    @MockK lateinit var productLikeRepository: ProductLikeRepository
    @MockK lateinit var productRepository: ProductRepository

    @InjectMockKs
    lateinit var productLikeService: ProductLikeService

    private fun createProduct(
        id: Long = 10L,
        name: String = "테스트 상품",
        price: Long = 50000L,
        categoryId: Long = 1L,
    ) = Product(
        name = name,
        price = price,
        categoryId = categoryId,
        id = id,
    )

    private fun createProductLike(
        userId: Long = 1L,
        productId: Long = 10L,
        id: Long = 1L,
    ) = ProductLike(userId = userId, productId = productId, id = id)

    @Nested
    inner class Toggle {

        @Test
        fun `상품이 존재하지 않을 시 PRODUCT_NOT_FOUND 예외 발생`() {
            // given
            every { productRepository.findById(99L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> { productLikeService.toggle(userId = 1L, productId = 99L) }
            assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `좋아요가 없을 때 toggle 호출 시 liked true 반환 및 save 1회 호출`() {
            // given
            val product = createProduct()
            every { productRepository.findById(10L) } returns Optional.of(product)
            every { productLikeRepository.findByUserIdAndProductId(1L, 10L) } returns null
            every { productLikeRepository.save(any()) } returns createProductLike()
            every { productLikeRepository.countByProductId(10L) } returns 1L

            // when
            val result = productLikeService.toggle(userId = 1L, productId = 10L)

            // then
            assertTrue(result.liked)
            assertEquals(10L, result.productId)
            assertEquals(1L, result.likeCount)
            verify(exactly = 1) { productLikeRepository.save(any()) }
        }

        @Test
        fun `좋아요가 이미 있을 때 toggle 호출 시 liked false 반환 및 delete 1회 호출`() {
            // given
            val product = createProduct()
            val existingLike = createProductLike()
            every { productRepository.findById(10L) } returns Optional.of(product)
            every { productLikeRepository.findByUserIdAndProductId(1L, 10L) } returns existingLike
            every { productLikeRepository.delete(existingLike) } returns Unit
            every { productLikeRepository.countByProductId(10L) } returns 0L

            // when
            val result = productLikeService.toggle(userId = 1L, productId = 10L)

            // then
            assertFalse(result.liked)
            assertEquals(10L, result.productId)
            assertEquals(0L, result.likeCount)
            verify(exactly = 1) { productLikeRepository.delete(existingLike) }
        }

        @Test
        fun `좋아요 취소 시 save를 호출하지 않음`() {
            // given
            val product = createProduct()
            val existingLike = createProductLike()
            every { productRepository.findById(10L) } returns Optional.of(product)
            every { productLikeRepository.findByUserIdAndProductId(1L, 10L) } returns existingLike
            every { productLikeRepository.delete(existingLike) } returns Unit
            every { productLikeRepository.countByProductId(10L) } returns 0L

            // when
            productLikeService.toggle(userId = 1L, productId = 10L)

            // then
            verify(exactly = 0) { productLikeRepository.save(any()) }
        }

        @Test
        fun `좋아요 추가 시 delete를 호출하지 않음`() {
            // given
            val product = createProduct()
            every { productRepository.findById(10L) } returns Optional.of(product)
            every { productLikeRepository.findByUserIdAndProductId(1L, 10L) } returns null
            every { productLikeRepository.save(any()) } returns createProductLike()
            every { productLikeRepository.countByProductId(10L) } returns 1L

            // when
            productLikeService.toggle(userId = 1L, productId = 10L)

            // then
            verify(exactly = 0) { productLikeRepository.delete(any()) }
        }

        @Test
        fun `toggle 후 likeCount는 productLikeRepository countByProductId 반환값과 일치`() {
            // given
            val product = createProduct()
            every { productRepository.findById(10L) } returns Optional.of(product)
            every { productLikeRepository.findByUserIdAndProductId(1L, 10L) } returns null
            every { productLikeRepository.save(any()) } returns createProductLike()
            every { productLikeRepository.countByProductId(10L) } returns 42L

            // when
            val result = productLikeService.toggle(userId = 1L, productId = 10L)

            // then
            assertEquals(42L, result.likeCount)
        }
    }

    @Nested
    inner class GetMyLikedProducts {

        @Test
        fun `좋아요한 상품이 있을 때 ProductResponse 페이지 반환`() {
            // given
            val userId = 1L
            val pageable = PageRequest.of(0, 20)
            val likes = listOf(
                createProductLike(userId = userId, productId = 10L),
                createProductLike(userId = userId, productId = 20L, id = 2L),
            )
            val products = listOf(
                createProduct(id = 10L, name = "상품A"),
                createProduct(id = 20L, name = "상품B"),
            )
            val productPage = PageImpl(products, pageable, products.size.toLong())

            every { productLikeRepository.findAllByUserId(userId) } returns likes
            every { productRepository.findByIdIn(listOf(10L, 20L), pageable) } returns productPage

            // when
            val result = productLikeService.getMyLikedProducts(userId = userId, pageable = pageable)

            // then
            assertEquals(2, result.totalElements)
            assertEquals("상품A", result.content[0].name)
            assertEquals("상품B", result.content[1].name)
        }

        @Test
        fun `좋아요한 상품이 없을 때 빈 페이지 반환`() {
            // given
            val userId = 1L
            val pageable = PageRequest.of(0, 20)
            every { productLikeRepository.findAllByUserId(userId) } returns emptyList()

            // when
            val result = productLikeService.getMyLikedProducts(userId = userId, pageable = pageable)

            // then
            assertTrue(result.isEmpty)
            assertEquals(0, result.totalElements)
        }

        @Test
        fun `좋아요 목록이 비어있을 때 productRepository를 조회하지 않음`() {
            // given
            val userId = 1L
            val pageable = PageRequest.of(0, 20)
            every { productLikeRepository.findAllByUserId(userId) } returns emptyList()

            // when
            productLikeService.getMyLikedProducts(userId = userId, pageable = pageable)

            // then
            verify(exactly = 0) { productRepository.findByIdIn(any(), any()) }
        }
    }

    @Nested
    inner class GetLikeStatus {

        @Test
        fun `전체 상품 ID에 대해 좋아요 여부 Map 반환`() {
            // given
            val userId = 1L
            val productIds = listOf(10L, 20L, 30L)
            every { productLikeRepository.findLikedProductIds(userId, productIds) } returns listOf(10L, 30L)

            // when
            val result = productLikeService.getLikeStatus(userId = userId, productIds = productIds)

            // then
            assertEquals(3, result.size)
            assertTrue(result[10L] == true)
            assertTrue(result[20L] == false)
            assertTrue(result[30L] == true)
        }

        @Test
        fun `모든 상품에 좋아요가 없을 때 전부 false로 반환`() {
            // given
            val userId = 1L
            val productIds = listOf(10L, 20L)
            every { productLikeRepository.findLikedProductIds(userId, productIds) } returns emptyList()

            // when
            val result = productLikeService.getLikeStatus(userId = userId, productIds = productIds)

            // then
            assertEquals(2, result.size)
            assertTrue(result.values.all { !it })
        }

        @Test
        fun `모든 상품에 좋아요가 있을 때 전부 true로 반환`() {
            // given
            val userId = 1L
            val productIds = listOf(10L, 20L)
            every { productLikeRepository.findLikedProductIds(userId, productIds) } returns listOf(10L, 20L)

            // when
            val result = productLikeService.getLikeStatus(userId = userId, productIds = productIds)

            // then
            assertEquals(2, result.size)
            assertTrue(result.values.all { it })
        }

        @Test
        fun `단일 상품 ID 조회 시에도 Map 형태로 반환`() {
            // given
            val userId = 1L
            val productIds = listOf(10L)
            every { productLikeRepository.findLikedProductIds(userId, productIds) } returns listOf(10L)

            // when
            val result = productLikeService.getLikeStatus(userId = userId, productIds = productIds)

            // then
            assertEquals(1, result.size)
            assertTrue(result[10L] == true)
        }

        @Test
        fun `빈 productIds 목록으로 호출 시 빈 Map 반환`() {
            // given
            val userId = 1L
            val productIds = emptyList<Long>()
            every { productLikeRepository.findLikedProductIds(userId, productIds) } returns emptyList()

            // when
            val result = productLikeService.getLikeStatus(userId = userId, productIds = productIds)

            // then
            assertTrue(result.isEmpty())
        }
    }
}
