package com.example.commerce.category.service

import com.example.commerce.category.dto.CategoryCreateRequest
import com.example.commerce.category.dto.CategoryUpdateRequest
import com.example.commerce.category.entity.Category
import com.example.commerce.category.repository.CategoryRepository
import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
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
import java.util.Optional

@ExtendWith(MockKExtension::class)
class CategoryServiceTest {

    @MockK lateinit var categoryRepository: CategoryRepository

    @InjectMockKs
    lateinit var categoryService: CategoryService

    private fun createCategory(id: Long = 1L, name: String = "상의", parent: Category? = null) =
        Category(name = name, parent = parent, displayOrder = 0, id = id)

    @Nested
    inner class Create {

        @Test
        fun `부모 없이 생성 시 대분류 카테고리 반환`() {
            val request = CategoryCreateRequest(name = "상의")
            val saved = createCategory(id = 1L, name = "상의")

            every { categoryRepository.save(any()) } returns saved

            val result = categoryService.create(request)

            assertEquals("상의", result.name)
            assertTrue(result.children.isEmpty())
            verify(exactly = 1) { categoryRepository.save(any()) }
        }

        @Test
        fun `유효한 parentId로 생성 시 소분류 카테고리 반환`() {
            val parent = createCategory(id = 1L, name = "상의")
            val request = CategoryCreateRequest(name = "반팔티셔츠", parentId = 1L)
            val saved = createCategory(id = 2L, name = "반팔티셔츠", parent = parent)

            every { categoryRepository.findById(1L) } returns Optional.of(parent)
            every { categoryRepository.save(any()) } returns saved

            val result = categoryService.create(request)

            assertEquals("반팔티셔츠", result.name)
        }

        @Test
        fun `존재하지 않는 parentId 사용 시 CATEGORY_NOT_FOUND 예외 발생`() {
            val request = CategoryCreateRequest(name = "반팔티셔츠", parentId = 999L)

            every { categoryRepository.findById(999L) } returns Optional.empty()

            val exception = assertThrows<CustomException> { categoryService.create(request) }
            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
            verify(exactly = 0) { categoryRepository.save(any()) }
        }
    }

    @Nested
    inner class GetTree {

        @Test
        fun `대분류 목록과 소분류 트리 반환`() {
            val parent = createCategory(id = 1L, name = "상의")
            val child = createCategory(id = 2L, name = "반팔티셔츠", parent = parent)
            parent.children.add(child)

            every { categoryRepository.findAllRoots() } returns listOf(parent)

            val result = categoryService.getTree()

            assertEquals(1, result.size)
            assertEquals("상의", result[0].name)
            assertEquals(1, result[0].children.size)
            assertEquals("반팔티셔츠", result[0].children[0].name)
        }

        @Test
        fun `카테고리 없으면 빈 리스트 반환`() {
            every { categoryRepository.findAllRoots() } returns emptyList()

            val result = categoryService.getTree()

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `정상 수정 시 변경된 카테고리 반환`() {
            val category = createCategory(id = 1L, name = "상의")
            val request = CategoryUpdateRequest(name = "상의류", displayOrder = 1)

            every { categoryRepository.findById(1L) } returns Optional.of(category)

            val result = categoryService.update(1L, request)

            assertEquals("상의류", result.name)
            assertEquals(1, result.displayOrder)
        }

        @Test
        fun `존재하지 않는 id 수정 시 CATEGORY_NOT_FOUND 예외 발생`() {
            val request = CategoryUpdateRequest(name = "상의류")

            every { categoryRepository.findById(999L) } returns Optional.empty()

            val exception = assertThrows<CustomException> { categoryService.update(999L, request) }
            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `소분류 없는 카테고리 삭제 성공`() {
            val category = createCategory(id = 1L)

            every { categoryRepository.findById(1L) } returns Optional.of(category)
            every { categoryRepository.existsByParent(category) } returns false
            every { categoryRepository.delete(category) } returns Unit

            categoryService.delete(1L)

            verify(exactly = 1) { categoryRepository.delete(category) }
        }

        @Test
        fun `소분류가 있는 대분류 삭제 시 CATEGORY_HAS_CHILDREN 예외 발생`() {
            val category = createCategory(id = 1L)

            every { categoryRepository.findById(1L) } returns Optional.of(category)
            every { categoryRepository.existsByParent(category) } returns true

            val exception = assertThrows<CustomException> { categoryService.delete(1L) }
            assertEquals(ErrorCode.CATEGORY_HAS_CHILDREN, exception.errorCode)
            verify(exactly = 0) { categoryRepository.delete(any()) }
        }

        @Test
        fun `존재하지 않는 id 삭제 시 CATEGORY_NOT_FOUND 예외 발생`() {
            every { categoryRepository.findById(999L) } returns Optional.empty()

            val exception = assertThrows<CustomException> { categoryService.delete(999L) }
            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
        }
    }
}
