package com.example.commerce.category.repository

import com.example.commerce.category.entity.Category
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest

@DataJpaTest(properties = ["spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"])
class CategoryRepositoryTest {

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @BeforeEach
    fun setUp() {
        categoryRepository.deleteAll()
    }

    private fun saveCategory(name: String, parent: Category? = null, displayOrder: Int = 0) =
        categoryRepository.save(Category(name = name, parent = parent, displayOrder = displayOrder))

    @Nested
    inner class FindAllRoots {

        @Test
        fun `대분류만 반환하고 displayOrder 순으로 정렬`() {
            val 하의 = saveCategory("하의", displayOrder = 2)
            val 상의 = saveCategory("상의", displayOrder = 1)
            saveCategory("반팔티셔츠", parent = 상의)

            val result = categoryRepository.findAllRoots()

            assertEquals(2, result.size)
            assertEquals("상의", result[0].name)
            assertEquals("하의", result[1].name)
        }

        @Test
        fun `카테고리 없으면 빈 리스트 반환`() {
            val result = categoryRepository.findAllRoots()

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class ExistsByParent {

        @Test
        fun `소분류가 있는 대분류는 true 반환`() {
            val parent = saveCategory("상의")
            saveCategory("반팔티셔츠", parent = parent)

            assertTrue(categoryRepository.existsByParent(parent))
        }

        @Test
        fun `소분류가 없는 대분류는 false 반환`() {
            val parent = saveCategory("상의")

            assertFalse(categoryRepository.existsByParent(parent))
        }
    }

    @Nested
    inner class Save {

        @Test
        fun `카테고리 저장 시 id 자동 생성 및 createdAt 설정`() {
            val category = saveCategory("상의")

            assertTrue(category.id > 0)
            assertNotNull(category.createdAt)
        }

        @Test
        fun `소분류 저장 시 부모 참조 유지`() {
            val parent = saveCategory("상의")
            val child = saveCategory("반팔티셔츠", parent = parent)

            val found = categoryRepository.findById(child.id).orElseThrow()
            assertEquals(parent.id, found.parent?.id)
        }
    }
}
