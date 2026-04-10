package com.example.commerce.order.service

import com.example.commerce.cart.entity.Cart
import com.example.commerce.cart.entity.CartItem
import com.example.commerce.cart.repository.CartRepository
import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderItem
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.order.repository.OrderRepository
import com.example.commerce.product.entity.Product
import com.example.commerce.product.entity.ProductOption
import com.example.commerce.product.repository.ProductOptionRepository
import com.example.commerce.product.repository.ProductRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Optional

@ExtendWith(MockKExtension::class)
class OrderServiceTest {

    @MockK lateinit var orderRepository: OrderRepository
    @MockK lateinit var cartRepository: CartRepository
    @MockK lateinit var productRepository: ProductRepository
    @MockK lateinit var productOptionRepository: ProductOptionRepository

    @InjectMockKs
    lateinit var orderService: OrderService

    private fun createProduct(id: Long = 1L) = Product(
        name = "테스트 상품", price = 10000L, categoryId = 1L, id = id,
    )

    private fun createOption(product: Product, stock: Int = 10, id: Long = 1L) =
        ProductOption(product = product, size = "M", color = "블랙", stock = stock, id = id)

    private fun createCart(userId: Long = 1L, id: Long = 1L) = Cart(userId = userId, id = id)

    private fun createCartItem(cart: Cart, product: Product, option: ProductOption, quantity: Int = 2, id: Long = 1L) =
        CartItem(
            cart = cart, productId = product.id, productOptionId = option.id,
            quantity = quantity, price = product.price, id = id,
        )

    private fun createOrder(userId: Long = 1L, status: OrderStatus = OrderStatus.PENDING, id: Long = 1L): Order {
        val product = createProduct()
        val option = createOption(product)
        val order = Order(userId = userId, totalAmount = 20000L, status = status, id = id)
        order.items.add(
            OrderItem(
                order = order, productId = product.id, productOptionId = option.id,
                productName = product.name, optionInfo = "M / 블랙", price = 10000L, quantity = 2,
            )
        )
        return order
    }

    @Nested
    inner class CreateOrder {

        @Test
        fun `장바구니 상품으로 주문 생성 성공`() {
            val product = createProduct()
            val option = createOption(product, stock = 10)
            val cart = createCart()
            cart.items.add(createCartItem(cart, product, option, quantity = 2))

            val orderSlot = slot<Order>()
            every { cartRepository.findByUserId(1L) } returns cart
            every { productOptionRepository.findByIdWithLock(1L) } returns option
            every { productRepository.findById(1L) } returns Optional.of(product)
            every { orderRepository.save(capture(orderSlot)) } answers { orderSlot.captured }

            val result = orderService.createOrder(1L)

            assertEquals(1L, result.userId)
            assertEquals(OrderStatus.PENDING, result.status)
            assertEquals(20000L, result.totalAmount)
            assertEquals(1, result.items.size)
            assertEquals(8, option.stock) // 10 - 2
            assertTrue(cart.items.isEmpty()) // 장바구니 비워짐
        }

        @Test
        fun `장바구니가 비어있으면 CART_EMPTY 예외 발생`() {
            every { cartRepository.findByUserId(1L) } returns createCart()

            val ex = assertThrows<CustomException> { orderService.createOrder(1L) }
            assertEquals(ErrorCode.CART_EMPTY, ex.errorCode)
        }

        @Test
        fun `장바구니가 null이면 CART_EMPTY 예외 발생`() {
            every { cartRepository.findByUserId(1L) } returns null

            val ex = assertThrows<CustomException> { orderService.createOrder(1L) }
            assertEquals(ErrorCode.CART_EMPTY, ex.errorCode)
        }

        @Test
        fun `재고 부족 시 OUT_OF_STOCK 예외 발생`() {
            val product = createProduct()
            val option = createOption(product, stock = 1)
            val cart = createCart()
            cart.items.add(createCartItem(cart, product, option, quantity = 5))

            every { cartRepository.findByUserId(1L) } returns cart
            every { productOptionRepository.findByIdWithLock(1L) } returns option
            every { productRepository.findById(1L) } returns Optional.of(product)

            val ex = assertThrows<CustomException> { orderService.createOrder(1L) }
            assertEquals(ErrorCode.OUT_OF_STOCK, ex.errorCode)
        }

        @Test
        fun `옵션 정보를 스냅샷으로 저장`() {
            val product = createProduct()
            val option = createOption(product, stock = 10)
            val cart = createCart()
            cart.items.add(createCartItem(cart, product, option, quantity = 1))

            val orderSlot = slot<Order>()
            every { cartRepository.findByUserId(1L) } returns cart
            every { productOptionRepository.findByIdWithLock(1L) } returns option
            every { productRepository.findById(1L) } returns Optional.of(product)
            every { orderRepository.save(capture(orderSlot)) } answers { orderSlot.captured }

            val result = orderService.createOrder(1L)

            assertEquals("테스트 상품", result.items[0].productName)
            assertEquals("M / 블랙", result.items[0].optionInfo)
        }
    }

    @Nested
    inner class GetOrder {

        @Test
        fun `본인 주문 단건 조회 성공`() {
            val order = createOrder()
            every { orderRepository.findById(1L) } returns Optional.of(order)

            val result = orderService.getOrder(userId = 1L, orderId = 1L)

            assertEquals(1L, result.id)
            assertEquals(OrderStatus.PENDING, result.status)
        }

        @Test
        fun `존재하지 않는 주문 조회 시 ORDER_NOT_FOUND 예외 발생`() {
            every { orderRepository.findById(999L) } returns Optional.empty()

            val ex = assertThrows<CustomException> { orderService.getOrder(1L, 999L) }
            assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `다른 사용자 주문 조회 시 ORDER_NOT_OWNED 예외 발생`() {
            val order = createOrder(userId = 2L)
            every { orderRepository.findById(1L) } returns Optional.of(order)

            val ex = assertThrows<CustomException> { orderService.getOrder(userId = 1L, orderId = 1L) }
            assertEquals(ErrorCode.ORDER_NOT_OWNED, ex.errorCode)
        }
    }

    @Nested
    inner class GetOrders {

        @Test
        fun `본인 주문 목록 조회 성공`() {
            val orders = listOf(createOrder(id = 1L), createOrder(id = 2L))
            every { orderRepository.findAllByUserIdOrderByCreatedAtDesc(1L) } returns orders

            val result = orderService.getOrders(1L)

            assertEquals(2, result.size)
        }

        @Test
        fun `주문 없으면 빈 목록 반환`() {
            every { orderRepository.findAllByUserIdOrderByCreatedAtDesc(1L) } returns emptyList()

            val result = orderService.getOrders(1L)

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class CancelOrder {

        @Test
        fun `PENDING 주문 취소 성공 및 재고 복원`() {
            val product = createProduct()
            val option = createOption(product, stock = 8)
            val order = createOrder(status = OrderStatus.PENDING)

            every { orderRepository.findById(1L) } returns Optional.of(order)
            every { productOptionRepository.findByIdWithLock(option.id) } returns option

            val result = orderService.cancelOrder(userId = 1L, orderId = 1L)

            assertEquals(OrderStatus.CANCELLED, result.status)
            assertEquals(10, option.stock) // 8 + 2
        }

        @Test
        fun `PAID 주문 취소 성공`() {
            val product = createProduct()
            val option = createOption(product, stock = 5)
            val order = createOrder(status = OrderStatus.PAID)

            every { orderRepository.findById(1L) } returns Optional.of(order)
            every { productOptionRepository.findByIdWithLock(option.id) } returns option

            val result = orderService.cancelOrder(userId = 1L, orderId = 1L)

            assertEquals(OrderStatus.CANCELLED, result.status)
        }

        @Test
        fun `SHIPPING 상태 주문 취소 시 ORDER_CANNOT_CANCEL 예외 발생`() {
            val order = createOrder(status = OrderStatus.SHIPPING)
            every { orderRepository.findById(1L) } returns Optional.of(order)

            val ex = assertThrows<CustomException> { orderService.cancelOrder(1L, 1L) }
            assertEquals(ErrorCode.ORDER_CANNOT_CANCEL, ex.errorCode)
        }

        @Test
        fun `DELIVERED 상태 주문 취소 시 ORDER_CANNOT_CANCEL 예외 발생`() {
            val order = createOrder(status = OrderStatus.DELIVERED)
            every { orderRepository.findById(1L) } returns Optional.of(order)

            val ex = assertThrows<CustomException> { orderService.cancelOrder(1L, 1L) }
            assertEquals(ErrorCode.ORDER_CANNOT_CANCEL, ex.errorCode)
        }

        @Test
        fun `CANCELLED 주문 재취소 시 ORDER_CANNOT_CANCEL 예외 발생`() {
            val order = createOrder(status = OrderStatus.CANCELLED)
            every { orderRepository.findById(1L) } returns Optional.of(order)

            val ex = assertThrows<CustomException> { orderService.cancelOrder(1L, 1L) }
            assertEquals(ErrorCode.ORDER_CANNOT_CANCEL, ex.errorCode)
        }

        @Test
        fun `다른 사용자 주문 취소 시 ORDER_NOT_OWNED 예외 발생`() {
            val order = createOrder(userId = 2L)
            every { orderRepository.findById(1L) } returns Optional.of(order)

            val ex = assertThrows<CustomException> { orderService.cancelOrder(userId = 1L, orderId = 1L) }
            assertEquals(ErrorCode.ORDER_NOT_OWNED, ex.errorCode)
        }
    }
}
