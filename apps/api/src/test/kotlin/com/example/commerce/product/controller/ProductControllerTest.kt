package com.example.commerce.product.controller

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.common.security.JwtProvider
import com.example.commerce.common.security.SecurityConfig
import com.example.commerce.product.dto.ProductCreateRequest
import com.example.commerce.product.dto.ProductOptionRequest
import com.example.commerce.product.dto.ProductOptionResponse
import com.example.commerce.product.dto.ProductResponse
import com.example.commerce.product.dto.ProductUpdateRequest
import com.example.commerce.product.service.ProductService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
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

@WebMvcTest(ProductController::class)
@Import(SecurityConfig::class)
class ProductControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var productService: ProductService
    @MockitoBean lateinit var jwtProvider: JwtProvider

    private val optionResponse = ProductOptionResponse(id = 1L, size = "M", color = "블랙", stock = 10)
    private val productResponse = ProductResponse(
        id = 1L, name = "테스트 상품", description = "설명", price = 10000L, categoryId = 1L,
        imageUrl = null, options = listOf(optionResponse),
    )

    private fun adminAuth() = authentication(
        UsernamePasswordAuthenticationToken(1L, null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
    )

    @Nested
    inner class Create {

        @Test
        fun `ADMIN 권한으로 상품 등록 시 201 반환`() {
            val request = ProductCreateRequest(
                name = "테스트 상품", price = 10000L, categoryId = 1L,
                options = listOf(ProductOptionRequest(size = "M", color = "블랙", stock = 10)),
            )
            given(productService.create(any())).willReturn(productResponse)

            mockMvc.post("/api/products") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isCreated() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.name") { value("테스트 상품") }
                jsonPath("$.data.options[0].size") { value("M") }
            }
        }

        @Test
        fun `미인증 요청 시 401 반환`() {
            val request = ProductCreateRequest(
                name = "테스트 상품", price = 10000L, categoryId = 1L,
                options = listOf(ProductOptionRequest(size = "M", color = "블랙", stock = 10)),
            )

            mockMvc.post("/api/products") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        fun `존재하지 않는 카테고리 사용 시 404 반환`() {
            val request = ProductCreateRequest(
                name = "테스트 상품", price = 10000L, categoryId = 999L,
                options = listOf(ProductOptionRequest(size = "M", color = "블랙", stock = 10)),
            )
            given(productService.create(any())).willThrow(CustomException(ErrorCode.CATEGORY_NOT_FOUND))

            mockMvc.post("/api/products") {
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
    inner class GetList {

        @Test
        fun `미인증으로 상품 목록 조회 시 200 반환`() {
            val page = PageImpl(listOf(productResponse))
            given(productService.getList(anyOrNull(), anyOrNull(), any())).willReturn(page)

            mockMvc.get("/api/products").andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.content[0].name") { value("테스트 상품") }
            }
        }
    }

    @Nested
    inner class GetOne {

        @Test
        fun `미인증으로 상품 단건 조회 시 200 반환`() {
            given(productService.getOne(1L)).willReturn(productResponse)

            mockMvc.get("/api/products/1").andExpect {
                status { isOk() }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.options[0].stock") { value(10) }
            }
        }

        @Test
        fun `존재하지 않는 상품 조회 시 404 반환`() {
            given(productService.getOne(999L)).willThrow(CustomException(ErrorCode.PRODUCT_NOT_FOUND))

            mockMvc.get("/api/products/999").andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("PRODUCT_NOT_FOUND") }
            }
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `ADMIN 권한으로 수정 시 200 반환`() {
            val request = ProductUpdateRequest(name = "수정 상품", price = 20000L, categoryId = 1L)
            val updated = productResponse.copy(name = "수정 상품", price = 20000L)
            given(productService.update(eq(1L), any())).willReturn(updated)

            mockMvc.put("/api/products/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("수정 상품") }
                jsonPath("$.data.price") { value(20000) }
            }
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `ADMIN 권한으로 삭제 시 200 반환`() {
            given(productService.delete(1L)).willAnswer { }

            mockMvc.delete("/api/products/1") {
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
            }
        }

        @Test
        fun `존재하지 않는 상품 삭제 시 404 반환`() {
            given(productService.delete(999L)).willThrow(CustomException(ErrorCode.PRODUCT_NOT_FOUND))

            mockMvc.delete("/api/products/999") {
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("PRODUCT_NOT_FOUND") }
            }
        }
    }
}
