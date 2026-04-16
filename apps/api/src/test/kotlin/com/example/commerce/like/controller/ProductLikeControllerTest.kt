package com.example.commerce.like.controller

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.common.security.JwtProvider
import com.example.commerce.common.security.SecurityConfig
import com.example.commerce.like.dto.LikeResponse
import com.example.commerce.like.service.ProductLikeService
import com.example.commerce.product.dto.ProductOptionResponse
import com.example.commerce.product.dto.ProductResponse
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(ProductLikeController::class)
@Import(SecurityConfig::class)
class ProductLikeControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var productLikeService: ProductLikeService
    @MockitoBean lateinit var jwtProvider: JwtProvider

    private fun userAuth(userId: Long = 1L) = authentication(
        UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    )

    private fun createLikeResponse(
        productId: Long = 10L,
        liked: Boolean = true,
        likeCount: Long = 5L,
    ) = LikeResponse(
        productId = productId,
        liked = liked,
        likeCount = likeCount,
    )

    private fun createProductResponse(
        id: Long = 10L,
        name: String = "테스트 상품",
        price: Long = 50000L,
        categoryId: Long = 1L,
    ) = ProductResponse(
        id = id,
        name = name,
        description = "상품 설명",
        price = price,
        categoryId = categoryId,
        imageUrl = "https://example.com/image.jpg",
        options = listOf(
            ProductOptionResponse(id = 1L, size = "M", color = "블랙", stock = 10)
        ),
    )

    @Nested
    inner class Toggle {

        @Test
        fun `인증된 사용자가 좋아요 토글 시 200과 좋아요 상태 반환`() {
            val response = createLikeResponse(liked = true, likeCount = 6L)
            given(productLikeService.toggle(eq(1L), eq(10L))).willReturn(response)

            mockMvc.post("/api/likes/10") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.productId") { value(10) }
                jsonPath("$.data.liked") { value(true) }
                jsonPath("$.data.likeCount") { value(6) }
            }
        }

        @Test
        fun `이미 좋아요한 상품 토글 시 좋아요 취소 상태 반환`() {
            val response = createLikeResponse(liked = false, likeCount = 4L)
            given(productLikeService.toggle(eq(1L), eq(10L))).willReturn(response)

            mockMvc.post("/api/likes/10") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.liked") { value(false) }
                jsonPath("$.data.likeCount") { value(4) }
            }
        }

        @Test
        fun `존재하지 않는 상품에 좋아요 시도 시 404 반환`() {
            given(productLikeService.toggle(any(), eq(999L))).willThrow(CustomException(ErrorCode.PRODUCT_NOT_FOUND))

            mockMvc.post("/api/likes/999") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("PRODUCT_NOT_FOUND") }
            }
        }

        @Test
        fun `인증 없이 좋아요 토글 시도 시 401 반환`() {
            mockMvc.post("/api/likes/10") {
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    inner class GetMyLikedProducts {

        @Test
        fun `인증된 사용자가 좋아요 목록 조회 시 200과 상품 목록 반환`() {
            val page = PageImpl(
                listOf(
                    createProductResponse(id = 10L, name = "상품A"),
                    createProductResponse(id = 20L, name = "상품B"),
                )
            )
            given(productLikeService.getMyLikedProducts(eq(1L), any())).willReturn(page)

            mockMvc.get("/api/likes/my") {
                with(userAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.content[0].id") { value(10) }
                jsonPath("$.data.content[0].name") { value("상품A") }
                jsonPath("$.data.content[1].id") { value(20) }
                jsonPath("$.data.content[1].name") { value("상품B") }
            }
        }

        @Test
        fun `좋아요한 상품이 없을 때 빈 목록과 200 반환`() {
            val emptyPage = PageImpl<ProductResponse>(emptyList())
            given(productLikeService.getMyLikedProducts(eq(1L), any())).willReturn(emptyPage)

            mockMvc.get("/api/likes/my") {
                with(userAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.content") { isEmpty() }
            }
        }

        @Test
        fun `인증 없이 좋아요 목록 조회 시도 시 401 반환`() {
            mockMvc.get("/api/likes/my").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    inner class GetLikeStatus {

        @Test
        fun `인증된 사용자가 복수 상품 좋아요 상태 조회 시 200과 상태 맵 반환`() {
            val statusMap = mapOf(1L to true, 2L to false, 3L to true)
            given(productLikeService.getLikeStatus(eq(1L), eq(listOf(1L, 2L, 3L)))).willReturn(statusMap)

            mockMvc.get("/api/likes/status?productIds=1,2,3") {
                with(userAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.1") { value(true) }
                jsonPath("$.data.2") { value(false) }
                jsonPath("$.data.3") { value(true) }
            }
        }

        @Test
        fun `단일 상품 좋아요 상태 조회 시 200과 해당 상태 반환`() {
            val statusMap = mapOf(1L to true)
            given(productLikeService.getLikeStatus(eq(1L), eq(listOf(1L)))).willReturn(statusMap)

            mockMvc.get("/api/likes/status?productIds=1") {
                with(userAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.1") { value(true) }
            }
        }

        @Test
        fun `인증 없이 좋아요 상태 조회 시도 시 401 반환`() {
            mockMvc.get("/api/likes/status?productIds=1,2,3").andExpect {
                status { isUnauthorized() }
            }
        }
    }
}
