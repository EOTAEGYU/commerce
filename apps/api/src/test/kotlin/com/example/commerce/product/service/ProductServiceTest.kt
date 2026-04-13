package com.example.commerce.product.service

import com.example.commerce.category.repository.CategoryRepository
import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.product.dto.ProductCreateRequest
import com.example.commerce.product.dto.ProductOptionRequest
import com.example.commerce.product.dto.ProductUpdateRequest
import com.example.commerce.product.entity.Product
import com.example.commerce.product.entity.ProductOption
import com.example.commerce.product.repository.ProductOptionRepository
import com.example.commerce.product.repository.ProductRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

@ExtendWith(MockKExtension::class)
class ProductServiceTest {

    @MockK lateinit var productRepository: ProductRepository
    @MockK lateinit var productOptionRepository: ProductOptionRepository
    @MockK lateinit var categoryRepository: CategoryRepository

    @InjectMockKs
    lateinit var productService: ProductService

    private fun createProduct(id: Long = 1L) = Product(
        name = "테스트 상품",
        description = "설명",
        price = 10000L,
        categoryId = 1L,
        id = id,
    ).also {
        it.options.add(ProductOption(product = it, size = "M", color = "블랙", stock = 10, id = 1L))
    }

    @Nested
    inner class Create {

        @Test
        fun `정상 등록 시 ProductResponse 반환`() {
            val request = ProductCreateRequest(
                name = "테스트 상품",
                price = 10000L,
                categoryId = 1L,
                options = listOf(ProductOptionRequest(size = "M", color = "블랙", stock = 10)),
            )
            val saved = createProduct()

            every { categoryRepository.existsById(1L) } returns true
            every { productRepository.save(any()) } returns saved

            val result = productService.create(request)

            assertEquals("테스트 상품", result.name)
            assertEquals(1, result.options.size)
            verify(exactly = 1) { productRepository.save(any()) }
        }

        @Test
        fun `존재하지 않는 categoryId 사용 시 CATEGORY_NOT_FOUND 예외 발생`() {
            val request = ProductCreateRequest(
                name = "테스트 상품",
                price = 10000L,
                categoryId = 999L,
                options = listOf(ProductOptionRequest(size = "M", color = "블랙", stock = 10)),
            )

            every { categoryRepository.existsById(999L) } returns false

            val exception = assertThrows<CustomException> { productService.create(request) }
            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
            verify(exactly = 0) { productRepository.save(any()) }
        }
    }

    @Nested
    inner class GetList {

        @Test
        fun `파라미터 없이 조회 시 전체 상품 페이지 반환`() {
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(createProduct()))

            every { productRepository.search(null, "%", pageable) } returns page

            val result = productService.getList(null, null, pageable)

            assertEquals(1, result.totalElements)
        }

        @Test
        fun `categoryId로 필터링 시 해당 카테고리 상품만 반환`() {
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(createProduct()))

            every { productRepository.search(1L, "%", pageable) } returns page

            val result = productService.getList(1L, null, pageable)

            assertEquals(1, result.totalElements)
        }

        @Test
        fun `keyword로 검색 시 이름 일치 상품만 반환`() {
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(createProduct()))

            every { productRepository.search(null, "%나이키%", pageable) } returns page

            val result = productService.getList(null, "나이키", pageable)

            assertEquals(1, result.totalElements)
        }

        @Test
        fun `categoryId와 keyword 조합 검색 시 결과 반환`() {
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(createProduct()))

            every { productRepository.search(1L, "%나이키%", pageable) } returns page

            val result = productService.getList(1L, "나이키", pageable)

            assertEquals(1, result.totalElements)
        }

        @Test
        fun `공백 keyword 입력 시 전체 조회와 동일하게 처리`() {
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(createProduct()))

            every { productRepository.search(null, "%", pageable) } returns page

            val result = productService.getList(null, "   ", pageable)

            assertEquals(1, result.totalElements)
        }
    }

    @Nested
    inner class GetOne {

        @Test
        fun `존재하는 id 조회 시 ProductResponse 반환`() {
            val product = createProduct(id = 1L)

            every { productRepository.findById(1L) } returns Optional.of(product)

            val result = productService.getOne(1L)

            assertEquals(1L, result.id)
            assertEquals("테스트 상품", result.name)
            assertEquals(1, result.options.size)
        }

        @Test
        fun `존재하지 않는 id 조회 시 PRODUCT_NOT_FOUND 예외 발생`() {
            every { productRepository.findById(999L) } returns Optional.empty()

            val exception = assertThrows<CustomException> { productService.getOne(999L) }
            assertEquals(ErrorCode.PRODUCT_NOT_FOUND, exception.errorCode)
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `정상 수정 시 변경된 ProductResponse 반환`() {
            val product = createProduct(id = 1L)
            val request = ProductUpdateRequest(name = "수정 상품", price = 20000L, categoryId = 1L)

            every { productRepository.findById(1L) } returns Optional.of(product)
            every { categoryRepository.existsById(1L) } returns true

            val result = productService.update(1L, request)

            assertEquals("수정 상품", result.name)
            assertEquals(20000L, result.price)
        }

        @Test
        fun `존재하지 않는 id 수정 시 PRODUCT_NOT_FOUND 예외 발생`() {
            val request = ProductUpdateRequest(name = "수정 상품", price = 20000L, categoryId = 1L)

            every { productRepository.findById(999L) } returns Optional.empty()

            val exception = assertThrows<CustomException> { productService.update(999L, request) }
            assertEquals(ErrorCode.PRODUCT_NOT_FOUND, exception.errorCode)
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `존재하는 상품 삭제 성공`() {
            every { productRepository.existsById(1L) } returns true
            every { productRepository.deleteById(1L) } returns Unit

            productService.delete(1L)

            verify(exactly = 1) { productRepository.deleteById(1L) }
        }

        @Test
        fun `존재하지 않는 id 삭제 시 PRODUCT_NOT_FOUND 예외 발생`() {
            every { productRepository.existsById(999L) } returns false

            val exception = assertThrows<CustomException> { productService.delete(999L) }
            assertEquals(ErrorCode.PRODUCT_NOT_FOUND, exception.errorCode)
        }
    }

    @Nested
    inner class DecreaseStock {

        @Test
        fun `재고 충분 시 차감 성공`() {
            val product = createProduct()
            val option = ProductOption(product = product, size = "M", color = "블랙", stock = 10, id = 1L)

            every { productOptionRepository.findByIdWithLock(1L) } returns option

            productService.decreaseStock(1L, 3)

            assertEquals(7, option.stock)
        }

        @Test
        fun `재고 부족 시 OUT_OF_STOCK 예외 발생`() {
            val product = createProduct()
            val option = ProductOption(product = product, size = "M", color = "블랙", stock = 2, id = 1L)

            every { productOptionRepository.findByIdWithLock(1L) } returns option

            val exception = assertThrows<CustomException> { productService.decreaseStock(1L, 5) }
            assertEquals(ErrorCode.OUT_OF_STOCK, exception.errorCode)
        }

        @Test
        fun `존재하지 않는 옵션 id 사용 시 PRODUCT_OPTION_NOT_FOUND 예외 발생`() {
            every { productOptionRepository.findByIdWithLock(999L) } returns null

            val exception = assertThrows<CustomException> { productService.decreaseStock(999L, 1) }
            assertEquals(ErrorCode.PRODUCT_OPTION_NOT_FOUND, exception.errorCode)
        }
    }

    @Nested
    inner class IncreaseStock {

        @Test
        fun `재고 복구 성공`() {
            val product = createProduct()
            val option = ProductOption(product = product, size = "M", color = "블랙", stock = 5, id = 1L)

            every { productOptionRepository.findByIdWithLock(1L) } returns option

            productService.increaseStock(1L, 3)

            assertEquals(8, option.stock)
        }
    }
}
