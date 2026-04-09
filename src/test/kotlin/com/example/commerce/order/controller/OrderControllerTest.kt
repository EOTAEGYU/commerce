package com.example.commerce.order.controller

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.common.security.JwtProvider
import com.example.commerce.common.security.SecurityConfig
import com.example.commerce.order.dto.OrderItemResponse
import com.example.commerce.order.dto.OrderResponse
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.order.service.OrderService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

@WebMvcTest(OrderController::class)
@Import(SecurityConfig::class)
class OrderControllerTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var orderService: OrderService
    @MockitoBean lateinit var jwtProvider: JwtProvider

    private val orderItemResponse = OrderItemResponse(
        id = 1L, productId = 1L, productOptionId = 1L,
        productName = "테스트 상품", optionInfo = "M / 블랙",
        price = 10000L, quantity = 2, totalPrice = 20000L,
    )
    private val orderResponse = OrderResponse(
        id = 1L, userId = 1L, status = OrderStatus.PENDING, totalAmount = 20000L,
        items = listOf(orderItemResponse), createdAt = LocalDateTime.now().toString(),
    )

    private fun userAuth() = authentication(
        UsernamePasswordAuthenticationToken(1L, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    )

    @Nested
    inner class CreateOrder {

        @Test
        fun `주문 생성 성공 시 200 반환`() {
            given(orderService.createOrder(1L)).willReturn(orderResponse)

            mockMvc.post("/api/orders") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.status") { value("PENDING") }
                jsonPath("$.data.totalAmount") { value(20000) }
                jsonPath("$.data.items[0].productName") { value("테스트 상품") }
            }
        }

        @Test
        fun `장바구니가 비어 있으면 400 반환`() {
            given(orderService.createOrder(1L)).willThrow(CustomException(ErrorCode.CART_EMPTY))

            mockMvc.post("/api/orders") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("CART_EMPTY") }
            }
        }

        @Test
        fun `재고 부족 시 409 반환`() {
            given(orderService.createOrder(1L)).willThrow(CustomException(ErrorCode.OUT_OF_STOCK))

            mockMvc.post("/api/orders") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("OUT_OF_STOCK") }
            }
        }

        @Test
        fun `미인증 요청 시 401 반환`() {
            mockMvc.post("/api/orders") {
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    inner class GetOrders {

        @Test
        fun `주문 목록 조회 성공 시 200 반환`() {
            given(orderService.getOrders(1L)).willReturn(listOf(orderResponse))

            mockMvc.get("/api/orders") {
                with(userAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].id") { value(1) }
                jsonPath("$.data[0].status") { value("PENDING") }
            }
        }

        @Test
        fun `미인증 요청 시 401 반환`() {
            mockMvc.get("/api/orders").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    inner class GetOrder {

        @Test
        fun `주문 단건 조회 성공 시 200 반환`() {
            given(orderService.getOrder(1L, 1L)).willReturn(orderResponse)

            mockMvc.get("/api/orders/1") {
                with(userAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.items[0].optionInfo") { value("M / 블랙") }
            }
        }

        @Test
        fun `존재하지 않는 주문 조회 시 404 반환`() {
            given(orderService.getOrder(1L, 999L)).willThrow(CustomException(ErrorCode.ORDER_NOT_FOUND))

            mockMvc.get("/api/orders/999") {
                with(userAuth())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("ORDER_NOT_FOUND") }
            }
        }

        @Test
        fun `다른 사용자 주문 조회 시 403 반환`() {
            given(orderService.getOrder(1L, 2L)).willThrow(CustomException(ErrorCode.ORDER_NOT_OWNED))

            mockMvc.get("/api/orders/2") {
                with(userAuth())
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error.code") { value("ORDER_NOT_OWNED") }
            }
        }
    }

    @Nested
    inner class CancelOrder {

        @Test
        fun `주문 취소 성공 시 200 반환`() {
            val cancelled = orderResponse.copy(status = OrderStatus.CANCELLED)
            given(orderService.cancelOrder(1L, 1L)).willReturn(cancelled)

            mockMvc.post("/api/orders/1/cancel") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("CANCELLED") }
            }
        }

        @Test
        fun `취소 불가 상태 주문 취소 시 409 반환`() {
            given(orderService.cancelOrder(1L, 1L)).willThrow(CustomException(ErrorCode.ORDER_CANNOT_CANCEL))

            mockMvc.post("/api/orders/1/cancel") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("ORDER_CANNOT_CANCEL") }
            }
        }

        @Test
        fun `미인증 요청 시 401 반환`() {
            mockMvc.post("/api/orders/1/cancel") {
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }
}
