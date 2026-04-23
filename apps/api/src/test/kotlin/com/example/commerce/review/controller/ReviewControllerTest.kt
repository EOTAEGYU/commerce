package com.example.commerce.review.controller

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.common.security.JwtProvider
import com.example.commerce.common.security.SecurityConfig
import com.example.commerce.common.security.oauth2.CustomOAuth2UserService
import com.example.commerce.common.security.oauth2.OAuth2FailureHandler
import com.example.commerce.common.security.oauth2.OAuth2SuccessHandler
import com.example.commerce.review.dto.ReviewCreateRequest
import com.example.commerce.review.dto.ReviewResponse
import com.example.commerce.review.dto.ReviewUpdateRequest
import com.example.commerce.review.service.ReviewService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
import java.time.LocalDateTime

@WebMvcTest(ReviewController::class)
@Import(SecurityConfig::class)
class ReviewControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var reviewService: ReviewService
    @MockitoBean lateinit var jwtProvider: JwtProvider
    @MockitoBean lateinit var customOAuth2UserService: CustomOAuth2UserService
    @MockitoBean lateinit var oauth2SuccessHandler: OAuth2SuccessHandler
    @MockitoBean lateinit var oauth2FailureHandler: OAuth2FailureHandler

    private val now = LocalDateTime.of(2026, 4, 16, 12, 0, 0)

    private fun createReviewResponse(
        id: Long = 1L,
        userId: Long = 1L,
        productId: Long = 10L,
        orderItemId: Long = 100L,
        rating: Double = 4.5,
        content: String = "좋은 상품입니다.",
    ) = ReviewResponse(
        id = id,
        userId = userId,
        userName = "테스터",
        productId = productId,
        orderItemId = orderItemId,
        rating = rating,
        content = content,
        createdAt = now,
        updatedAt = now,
    )

    private fun userAuth(userId: Long = 1L) = authentication(
        UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    )

    @Nested
    inner class CreateReview {

        @Test
        fun `정상 입력으로 리뷰 작성 시 201 반환`() {
            val request = ReviewCreateRequest(orderItemId = 100L, rating = 4.5, content = "좋은 상품입니다.")
            val response = createReviewResponse()
            given(reviewService.createReview(any(), any())).willReturn(response)

            mockMvc.post("/api/reviews") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isCreated() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.rating") { value(4.5) }
                jsonPath("$.data.content") { value("좋은 상품입니다.") }
            }
        }

        @Test
        fun `인증 없이 리뷰 작성 시도 시 401 반환`() {
            val request = ReviewCreateRequest(orderItemId = 100L, rating = 4.5, content = "좋은 상품입니다.")

            mockMvc.post("/api/reviews") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        fun `배송 완료 상태가 아닌 주문에 리뷰 작성 시 400 반환`() {
            val request = ReviewCreateRequest(orderItemId = 100L, rating = 4.5, content = "좋은 상품입니다.")
            given(reviewService.createReview(any(), any())).willThrow(CustomException(ErrorCode.ORDER_NOT_DELIVERED))

            mockMvc.post("/api/reviews") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("ORDER_NOT_DELIVERED") }
            }
        }

        @Test
        fun `이미 리뷰가 존재하는 OrderItem에 작성 시 409 반환`() {
            val request = ReviewCreateRequest(orderItemId = 100L, rating = 4.5, content = "좋은 상품입니다.")
            given(reviewService.createReview(any(), any())).willThrow(CustomException(ErrorCode.REVIEW_ALREADY_EXISTS))

            mockMvc.post("/api/reviews") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("REVIEW_ALREADY_EXISTS") }
            }
        }
    }

    @Nested
    inner class UpdateReview {

        @Test
        fun `본인 리뷰 정상 수정 시 200 반환`() {
            val request = ReviewUpdateRequest(rating = 3.5, content = "다시 생각해보니 보통입니다.")
            val updated = createReviewResponse(rating = 3.5, content = "다시 생각해보니 보통입니다.")
            given(reviewService.updateReview(any(), eq(1L), any())).willReturn(updated)

            mockMvc.put("/api/reviews/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.rating") { value(3.5) }
                jsonPath("$.data.content") { value("다시 생각해보니 보통입니다.") }
            }
        }

        @Test
        fun `존재하지 않는 리뷰 수정 시도 시 404 반환`() {
            val request = ReviewUpdateRequest(rating = 3.5, content = "수정 내용")
            given(reviewService.updateReview(any(), eq(999L), any())).willThrow(CustomException(ErrorCode.REVIEW_NOT_FOUND))

            mockMvc.put("/api/reviews/999") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("REVIEW_NOT_FOUND") }
            }
        }

        @Test
        fun `타인의 리뷰 수정 시도 시 403 반환`() {
            val request = ReviewUpdateRequest(rating = 3.5, content = "수정 내용")
            given(reviewService.updateReview(any(), eq(1L), any())).willThrow(CustomException(ErrorCode.REVIEW_NOT_OWNED))

            mockMvc.put("/api/reviews/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(userAuth(userId = 2L))
                with(csrf())
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error.code") { value("REVIEW_NOT_OWNED") }
            }
        }

        @Test
        fun `인증 없이 리뷰 수정 시도 시 401 반환`() {
            val request = ReviewUpdateRequest(rating = 3.5, content = "수정 내용")

            mockMvc.put("/api/reviews/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    inner class DeleteReview {

        @Test
        fun `본인 리뷰 정상 삭제 시 200 반환`() {
            given(reviewService.deleteReview(any(), eq(1L))).willAnswer { }

            mockMvc.delete("/api/reviews/1") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
            }
        }

        @Test
        fun `존재하지 않는 리뷰 삭제 시도 시 404 반환`() {
            given(reviewService.deleteReview(any(), eq(999L))).willThrow(CustomException(ErrorCode.REVIEW_NOT_FOUND))

            mockMvc.delete("/api/reviews/999") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("REVIEW_NOT_FOUND") }
            }
        }

        @Test
        fun `타인의 리뷰 삭제 시도 시 403 반환`() {
            given(reviewService.deleteReview(any(), eq(1L))).willThrow(CustomException(ErrorCode.REVIEW_NOT_OWNED))

            mockMvc.delete("/api/reviews/1") {
                with(userAuth(userId = 2L))
                with(csrf())
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error.code") { value("REVIEW_NOT_OWNED") }
            }
        }

        @Test
        fun `인증 없이 삭제 시도 시 401 반환`() {
            mockMvc.delete("/api/reviews/1") {
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    inner class GetProductReviews {

        @Test
        fun `미인증으로 상품별 리뷰 목록 조회 시 200 반환`() {
            val page = PageImpl(listOf(createReviewResponse()))
            given(reviewService.getProductReviews(eq(10L), any())).willReturn(page)

            mockMvc.get("/api/products/10/reviews").andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.content[0].id") { value(1) }
                jsonPath("$.data.content[0].rating") { value(4.5) }
                jsonPath("$.data.content[0].productId") { value(10) }
            }
        }

        @Test
        fun `리뷰가 없는 상품 조회 시 빈 목록과 200 반환`() {
            val emptyPage = PageImpl<ReviewResponse>(emptyList())
            given(reviewService.getProductReviews(eq(10L), any())).willReturn(emptyPage)

            mockMvc.get("/api/products/10/reviews").andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.content") { isEmpty() }
            }
        }
    }

    @Nested
    inner class GetMyReviews {

        @Test
        fun `인증된 사용자가 내 리뷰 목록 조회 시 200 반환`() {
            val reviews = listOf(createReviewResponse(id = 1L), createReviewResponse(id = 2L, orderItemId = 200L))
            given(reviewService.getMyReviews(any())).willReturn(reviews)

            mockMvc.get("/api/reviews/my") {
                with(userAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data[0].id") { value(1) }
                jsonPath("$.data[1].id") { value(2) }
            }
        }

        @Test
        fun `리뷰가 없는 사용자의 내 리뷰 목록 조회 시 빈 목록과 200 반환`() {
            given(reviewService.getMyReviews(any())).willReturn(emptyList())

            mockMvc.get("/api/reviews/my") {
                with(userAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { isEmpty() }
            }
        }

        @Test
        fun `인증 없이 내 리뷰 목록 조회 시도 시 401 반환`() {
            mockMvc.get("/api/reviews/my").andExpect {
                status { isUnauthorized() }
            }
        }
    }
}
