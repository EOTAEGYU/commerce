package com.example.commerce.product.repository

import com.example.commerce.category.entity.Category
import com.example.commerce.category.repository.CategoryRepository
import com.example.commerce.product.entity.Product
import com.example.commerce.product.entity.ProductOption
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.PageRequest

@DataJpaTest(properties = ["spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"])
class ProductRepositoryTest {

    @Autowired lateinit var productRepository: ProductRepository
    @Autowired lateinit var productOptionRepository: ProductOptionRepository
    @Autowired lateinit var categoryRepository: CategoryRepository

    private lateinit var category: Category

    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
        categoryRepository.deleteAll()
        category = categoryRepository.save(Category(name = "상의"))
    }

    private fun saveProduct(name: String = "테스트 상품", categoryId: Long = category.id) =
        productRepository.save(Product(name = name, price = 10000L, categoryId = categoryId))

    @Nested
    inner class Search {

        @Test
        fun `categoryId 없이 조회 시 전체 반환`() {
            saveProduct("상의 상품")
            saveProduct("하의 상품")

            val result = productRepository.search(null, "%", PageRequest.of(0, 20))

            assertEquals(2, result.totalElements)
        }

        @Test
        fun `categoryId로 필터링 시 해당 카테고리 상품만 반환`() {
            val other = categoryRepository.save(Category(name = "하의"))
            saveProduct("상의 상품", category.id)
            saveProduct("하의 상품", other.id)

            val result = productRepository.search(category.id, "%", PageRequest.of(0, 20))

            assertEquals(1, result.totalElements)
            assertEquals("상의 상품", result.content[0].name)
        }

        @Test
        fun `keyword로 이름 검색 시 일치 상품만 반환`() {
            saveProduct("나이키 티셔츠")
            saveProduct("아디다스 바지")

            val result = productRepository.search(null, "%나이키%", PageRequest.of(0, 20))

            assertEquals(1, result.totalElements)
            assertEquals("나이키 티셔츠", result.content[0].name)
        }

        @Test
        fun `keyword 대소문자 구분 없이 검색`() {
            saveProduct("Nike 티셔츠")

            val result = productRepository.search(null, "%nike%", PageRequest.of(0, 20))

            assertEquals(1, result.totalElements)
        }

        @Test
        fun `해당 카테고리 상품 없으면 빈 페이지 반환`() {
            val result = productRepository.search(category.id, "%", PageRequest.of(0, 20))

            assertEquals(0, result.totalElements)
        }
    }

    @Nested
    inner class ExistsByCategoryId {

        @Test
        fun `해당 카테고리에 상품 있으면 true 반환`() {
            saveProduct()

            assertTrue(productRepository.existsByCategoryId(category.id))
        }

        @Test
        fun `해당 카테고리에 상품 없으면 false 반환`() {
            assertFalse(productRepository.existsByCategoryId(category.id))
        }
    }

    @Nested
    inner class SaveWithOptions {

        @Test
        fun `상품 저장 시 옵션 함께 저장`() {
            val product = saveProduct()
            product.options.add(ProductOption(product = product, size = "M", color = "블랙", stock = 10))
            productRepository.save(product)

            val found = productRepository.findById(product.id).orElseThrow()
            assertEquals(1, found.options.size)
            assertEquals("M", found.options[0].size)
            assertEquals(10, found.options[0].stock)
        }
    }
}
