package com.example.commerce.payment.controller

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.common.security.JwtProvider
import com.example.commerce.common.security.SecurityConfig
import com.example.commerce.common.security.oauth2.CustomOAuth2UserService
import com.example.commerce.common.security.oauth2.OAuth2FailureHandler
import com.example.commerce.common.security.oauth2.OAuth2SuccessHandler
import com.example.commerce.payment.dto.PaymentRequest
import com.example.commerce.payment.dto.PaymentResponse
import com.example.commerce.payment.entity.PaymentMethod
import com.example.commerce.payment.entity.PaymentStatus
import com.example.commerce.payment.service.PaymentService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

@WebMvcTest(PaymentController::class)
@Import(SecurityConfig::class)
class PaymentControllerTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var paymentService: PaymentService
    @MockitoBean lateinit var jwtProvider: JwtProvider
    @MockitoBean lateinit var customOAuth2UserService: CustomOAuth2UserService
    @MockitoBean lateinit var oauth2SuccessHandler: OAuth2SuccessHandler
    @MockitoBean lateinit var oauth2FailureHandler: OAuth2FailureHandler

    private val paymentResponse = PaymentResponse(
        id = 1L,
        orderId = 1L,
        userId = 1L,
        amount = 20000L,
        discountAmount = 0L,
        originalAmount = 20000L,
        couponId = null,
        method = PaymentMethod.CARD,
        status = PaymentStatus.COMPLETED,
        pgTransactionId = "pg-uuid-1234",
        createdAt = LocalDateTime.now().toString(),
        pointAmount = 0L,
        earnedPoints = 200L,
    )

    private fun userAuth() = authentication(
        UsernamePasswordAuthenticationToken(1L, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    )

    @Nested
    inner class RequestPayment {

        @Test
        fun `결제 성공 시 200 반환`() {
            given(paymentService.requestPayment(any(), any())).willReturn(paymentResponse)

            mockMvc.post("/api/payments") {
                with(userAuth())
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"orderId": 1, "method": "CARD", "simulateFailure": false}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.status") { value("COMPLETED") }
                jsonPath("$.data.pgTransactionId") { value("pg-uuid-1234") }
            }
        }

        @Test
        fun `결제 실패(simulateFailure) 시 FAILED 상태 반환`() {
            val failedResponse = paymentResponse.copy(status = PaymentStatus.FAILED, pgTransactionId = null)
            given(paymentService.requestPayment(any(), any())).willReturn(failedResponse)

            mockMvc.post("/api/payments") {
                with(userAuth())
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"orderId": 1, "method": "CARD", "simulateFailure": true}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("FAILED") }
            }
        }

        @Test
        fun `존재하지 않는 주문 결제 시 404 반환`() {
            given(paymentService.requestPayment(any(), any()))
                .willThrow(CustomException(ErrorCode.ORDER_NOT_FOUND))

            mockMvc.post("/api/payments") {
                with(userAuth())
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"orderId": 999, "method": "CARD"}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("ORDER_NOT_FOUND") }
            }
        }

        @Test
        fun `결제 불가 주문 상태 시 409 반환`() {
            given(paymentService.requestPayment(any(), any()))
                .willThrow(CustomException(ErrorCode.ORDER_NOT_PAYABLE))

            mockMvc.post("/api/payments") {
                with(userAuth())
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"orderId": 1, "method": "CARD"}"""
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("ORDER_NOT_PAYABLE") }
            }
        }

        @Test
        fun `중복 결제 시 409 반환`() {
            given(paymentService.requestPayment(any(), any()))
                .willThrow(CustomException(ErrorCode.ORDER_ALREADY_PAID))

            mockMvc.post("/api/payments") {
                with(userAuth())
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"orderId": 1, "method": "CARD"}"""
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("ORDER_ALREADY_PAID") }
            }
        }

        @Test
        fun `미인증 요청 시 401 반환`() {
            mockMvc.post("/api/payments") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"orderId": 1, "method": "CARD"}"""
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    inner class GetPayment {

        @Test
        fun `결제 내역 조회 성공 시 200 반환`() {
            given(paymentService.getPayment(1L, 1L)).willReturn(paymentResponse)

            mockMvc.get("/api/payments/1") {
                with(userAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.orderId") { value(1) }
                jsonPath("$.data.status") { value("COMPLETED") }
                jsonPath("$.data.amount") { value(20000) }
            }
        }

        @Test
        fun `결제 내역 없으면 404 반환`() {
            given(paymentService.getPayment(1L, 999L))
                .willThrow(CustomException(ErrorCode.PAYMENT_NOT_FOUND))

            mockMvc.get("/api/payments/999") {
                with(userAuth())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("PAYMENT_NOT_FOUND") }
            }
        }

        @Test
        fun `다른 사용자 결제 내역 조회 시 403 반환`() {
            given(paymentService.getPayment(1L, 2L))
                .willThrow(CustomException(ErrorCode.ORDER_NOT_OWNED))

            mockMvc.get("/api/payments/2") {
                with(userAuth())
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error.code") { value("ORDER_NOT_OWNED") }
            }
        }

        @Test
        fun `미인증 요청 시 401 반환`() {
            mockMvc.get("/api/payments/1").andExpect {
                status { isUnauthorized() }
            }
        }
    }
}
