package com.example.commerce.category.controller

import com.example.commerce.category.dto.CategoryCreateRequest
import com.example.commerce.category.dto.CategoryResponse
import com.example.commerce.category.dto.CategoryUpdateRequest
import com.example.commerce.category.service.CategoryService
import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.common.security.JwtProvider
import com.example.commerce.common.security.SecurityConfig
import com.example.commerce.common.security.oauth2.CustomOAuth2UserService
import com.example.commerce.common.security.oauth2.OAuth2FailureHandler
import com.example.commerce.common.security.oauth2.OAuth2SuccessHandler
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tools.jackson.databind.ObjectMapper

@WebMvcTest(CategoryController::class)
@Import(SecurityConfig::class)
class CategoryControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var categoryService: CategoryService
    @MockitoBean lateinit var jwtProvider: JwtProvider
    @MockitoBean lateinit var customOAuth2UserService: CustomOAuth2UserService
    @MockitoBean lateinit var oauth2SuccessHandler: OAuth2SuccessHandler
    @MockitoBean lateinit var oauth2FailureHandler: OAuth2FailureHandler

    private val parentResponse = CategoryResponse(id = 1L, name = "상의", displayOrder = 0, children = emptyList())
    private val childResponse = CategoryResponse(id = 2L, name = "반팔티셔츠", displayOrder = 0, children = emptyList())
    private val treeResponse = listOf(parentResponse.copy(children = listOf(childResponse)))

    private fun adminAuth() = authentication(
        UsernamePasswordAuthenticationToken(1L, null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
    )

    @Nested
    inner class Create {

        @Test
        fun `ADMIN 권한으로 대분류 생성 시 201 반환`() {
            val request = CategoryCreateRequest(name = "상의")
            given(categoryService.create(any())).willReturn(parentResponse)

            mockMvc.post("/api/categories") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isCreated() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.name") { value("상의") }
            }
        }

        @Test
        fun `미인증 요청 시 401 반환`() {
            val request = CategoryCreateRequest(name = "상의")

            mockMvc.post("/api/categories") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        fun `name 빈 값으로 요청 시 400 반환`() {
            val request = CategoryCreateRequest(name = "")

            mockMvc.post("/api/categories") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        fun `존재하지 않는 parentId 사용 시 404 반환`() {
            val request = CategoryCreateRequest(name = "반팔티셔츠", parentId = 999L)
            given(categoryService.create(any())).willThrow(CustomException(ErrorCode.CATEGORY_NOT_FOUND))

            mockMvc.post("/api/categories") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("CATEGORY_NOT_FOUND") }
            }
        }
    }

    @Nested
    inner class GetTree {

        @Test
        fun `미인증 요청으로 카테고리 트리 조회 시 200 반환`() {
            given(categoryService.getTree()).willReturn(treeResponse)

            mockMvc.get("/api/categories").andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data[0].name") { value("상의") }
                jsonPath("$.data[0].children[0].name") { value("반팔티셔츠") }
            }
        }

        @Test
        fun `카테고리 없으면 빈 배열 반환`() {
            given(categoryService.getTree()).willReturn(emptyList())

            mockMvc.get("/api/categories").andExpect {
                status { isOk() }
                jsonPath("$.data") { isEmpty() }
            }
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `ADMIN 권한으로 수정 시 200 반환`() {
            val request = CategoryUpdateRequest(name = "상의류")
            val updated = parentResponse.copy(name = "상의류")
            given(categoryService.update(eq(1L), any())).willReturn(updated)

            mockMvc.put("/api/categories/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("상의류") }
            }
        }

        @Test
        fun `존재하지 않는 id 수정 시 404 반환`() {
            val request = CategoryUpdateRequest(name = "상의류")
            given(categoryService.update(eq(999L), any())).willThrow(CustomException(ErrorCode.CATEGORY_NOT_FOUND))

            mockMvc.put("/api/categories/999") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("CATEGORY_NOT_FOUND") }
            }
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `ADMIN 권한으로 삭제 시 200 반환`() {
            given(categoryService.delete(1L)).willAnswer { }

            mockMvc.delete("/api/categories/1") {
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
            }
        }

        @Test
        fun `소분류가 있는 카테고리 삭제 시 409 반환`() {
            given(categoryService.delete(1L)).willThrow(CustomException(ErrorCode.CATEGORY_HAS_CHILDREN))

            mockMvc.delete("/api/categories/1") {
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("CATEGORY_HAS_CHILDREN") }
            }
        }
    }
}
