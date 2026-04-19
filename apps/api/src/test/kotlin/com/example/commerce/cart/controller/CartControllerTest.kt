package com.example.commerce.cart.controller

import com.example.commerce.cart.dto.CartItemAddRequest
import com.example.commerce.cart.dto.CartItemResponse
import com.example.commerce.cart.dto.CartItemUpdateRequest
import com.example.commerce.cart.dto.CartResponse
import com.example.commerce.cart.service.CartService
import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.common.security.JwtProvider
import com.example.commerce.common.security.SecurityConfig
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

@WebMvcTest(CartController::class)
@Import(SecurityConfig::class)
class CartControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var cartService: CartService
    @MockitoBean lateinit var jwtProvider: JwtProvider

    private val cartItemResponse = CartItemResponse(
        id = 1L, productId = 1L, productOptionId = 1L, quantity = 2, price = 10000L, totalPrice = 20000L,
    )
    private val cartResponse = CartResponse(
        id = 1L, userId = 1L, items = listOf(cartItemResponse), totalAmount = 20000L,
    )

    private fun userAuth() = authentication(
        UsernamePasswordAuthenticationToken(1L, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    )

    @Nested
    inner class GetCart {

        @Test
        fun `인증된 사용자 장바구니 조회 시 200 반환`() {
            given(cartService.getCart(1L)).willReturn(cartResponse)

            mockMvc.get("/api/cart") {
                with(userAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.userId") { value(1) }
                jsonPath("$.data.totalAmount") { value(20000) }
                jsonPath("$.data.items[0].quantity") { value(2) }
            }
        }

        @Test
        fun `미인증 요청 시 401 반환`() {
            mockMvc.get("/api/cart").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    inner class AddItem {

        @Test
        fun `아이템 추가 성공 시 200 반환`() {
            val request = CartItemAddRequest(productId = 1L, productOptionId = 1L, quantity = 2)
            given(cartService.addItem(eq(1L), any())).willReturn(cartResponse)

            mockMvc.post("/api/cart/items") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.items[0].productId") { value(1) }
            }
        }

        @Test
        fun `재고 부족 시 409 반환`() {
            val request = CartItemAddRequest(productId = 1L, productOptionId = 1L, quantity = 100)
            given(cartService.addItem(eq(1L), any())).willThrow(CustomException(ErrorCode.OUT_OF_STOCK))

            mockMvc.post("/api/cart/items") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("OUT_OF_STOCK") }
            }
        }

        @Test
        fun `미인증 요청 시 401 반환`() {
            val request = CartItemAddRequest(productId = 1L, productOptionId = 1L, quantity = 1)

            mockMvc.post("/api/cart/items") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    inner class UpdateItem {

        @Test
        fun `수량 변경 성공 시 200 반환`() {
            val request = CartItemUpdateRequest(quantity = 5)
            val updated = cartResponse.copy(items = listOf(cartItemResponse.copy(quantity = 5, totalPrice = 50000L)), totalAmount = 50000L)
            given(cartService.updateItem(eq(1L), eq(1L), any())).willReturn(updated)

            mockMvc.put("/api/cart/items/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.items[0].quantity") { value(5) }
                jsonPath("$.data.totalAmount") { value(50000) }
            }
        }

        @Test
        fun `다른 사용자 아이템 수정 시 403 반환`() {
            val request = CartItemUpdateRequest(quantity = 1)
            given(cartService.updateItem(eq(1L), eq(1L), any())).willThrow(CustomException(ErrorCode.CART_ITEM_NOT_OWNED))

            mockMvc.put("/api/cart/items/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error.code") { value("CART_ITEM_NOT_OWNED") }
            }
        }
    }

    @Nested
    inner class DeleteItem {

        @Test
        fun `아이템 삭제 성공 시 200 반환`() {
            val emptyCart = CartResponse(id = 1L, userId = 1L, items = emptyList(), totalAmount = 0L)
            given(cartService.deleteItem(eq(1L), eq(1L))).willReturn(emptyCart)

            mockMvc.delete("/api/cart/items/1") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.items") { isEmpty() }
                jsonPath("$.data.totalAmount") { value(0) }
            }
        }

        @Test
        fun `존재하지 않는 아이템 삭제 시 404 반환`() {
            given(cartService.deleteItem(eq(1L), eq(999L))).willThrow(CustomException(ErrorCode.CART_ITEM_NOT_FOUND))

            mockMvc.delete("/api/cart/items/999") {
                with(userAuth())
                with(csrf())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("CART_ITEM_NOT_FOUND") }
            }
        }
    }
}
