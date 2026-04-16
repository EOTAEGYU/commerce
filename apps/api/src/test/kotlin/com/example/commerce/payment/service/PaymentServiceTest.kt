package com.example.commerce.payment.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderItem
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.order.repository.OrderRepository
import com.example.commerce.payment.dto.PaymentRequest
import com.example.commerce.payment.entity.Payment
import com.example.commerce.payment.entity.PaymentMethod
import com.example.commerce.payment.entity.PaymentStatus
import com.example.commerce.payment.repository.PaymentRepository
import com.example.commerce.product.entity.Product
import com.example.commerce.product.entity.ProductOption
import com.example.commerce.product.repository.ProductOptionRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Optional

@ExtendWith(MockKExtension::class)
class PaymentServiceTest {

    @MockK lateinit var orderRepository: OrderRepository
    @MockK lateinit var paymentRepository: PaymentRepository
    @MockK lateinit var productOptionRepository: ProductOptionRepository

    @InjectMockKs
    lateinit var paymentService: PaymentService

    private fun createProduct(id: Long = 1L) = Product(
        name = "테스트 상품", price = 10000L, categoryId = 1L, id = id,
    )

    private fun createOption(product: Product, stock: Int = 5, id: Long = 1L) =
        ProductOption(product = product, size = "M", color = "블랙", stock = stock, id = id)

    private fun createOrder(
        userId: Long = 1L,
        status: OrderStatus = OrderStatus.PENDING,
        id: Long = 1L,
    ): Order {
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

    private fun createPayment(
        orderId: Long = 1L,
        userId: Long = 1L,
        status: PaymentStatus = PaymentStatus.REQUESTED,
        id: Long = 1L,
    ) = Payment(
        orderId = orderId, userId = userId, amount = 20000L,
        method = PaymentMethod.CARD, status = status, id = id,
    )

    @Nested
    inner class RequestPayment {

        @Test
        fun `결제 성공 시 order PAID, payment COMPLETED`() {
            val order = createOrder()
            val paymentSlot = slot<Payment>()
            every { orderRepository.findById(1L) } returns Optional.of(order)
            every { paymentRepository.findByOrderId(1L) } returns null
            every { paymentRepository.save(capture(paymentSlot)) } answers { paymentSlot.captured }

            val result = paymentService.requestPayment(
                userId = 1L,
                request = PaymentRequest(orderId = 1L, method = PaymentMethod.CARD, simulateFailure = false),
            )

            assertEquals(PaymentStatus.COMPLETED, result.status)
            assertNotNull(result.pgTransactionId)
            assertEquals(OrderStatus.PAID, order.status)
        }

        @Test
        fun `결제 실패 시 재고 복원, order CANCELLED, payment FAILED`() {
            val product = createProduct()
            val option = createOption(product, stock = 5)
            val order = createOrder()
            val paymentSlot = slot<Payment>()

            every { orderRepository.findById(1L) } returns Optional.of(order)
            every { paymentRepository.findByOrderId(1L) } returns null
            every { paymentRepository.save(capture(paymentSlot)) } answers { paymentSlot.captured }
            every { productOptionRepository.findByIdWithLock(1L) } returns option

            val result = paymentService.requestPayment(
                userId = 1L,
                request = PaymentRequest(orderId = 1L, method = PaymentMethod.CARD, simulateFailure = true),
            )

            assertEquals(PaymentStatus.FAILED, result.status)
            assertNull(result.pgTransactionId)
            assertEquals(OrderStatus.CANCELLED, order.status)
            assertEquals(7, option.stock) // 5 + 2
        }

        @Test
        fun `존재하지 않는 주문 결제 시 ORDER_NOT_FOUND`() {
            every { orderRepository.findById(999L) } returns Optional.empty()

            val ex = assertThrows<CustomException> {
                paymentService.requestPayment(1L, PaymentRequest(999L, PaymentMethod.CARD))
            }
            assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `다른 사용자 주문 결제 시 ORDER_NOT_OWNED`() {
            val order = createOrder(userId = 2L)
            every { orderRepository.findById(1L) } returns Optional.of(order)

            val ex = assertThrows<CustomException> {
                paymentService.requestPayment(1L, PaymentRequest(1L, PaymentMethod.CARD))
            }
            assertEquals(ErrorCode.ORDER_NOT_OWNED, ex.errorCode)
        }

        @Test
        fun `PENDING 아닌 주문 결제 시 ORDER_NOT_PAYABLE`() {
            val order = createOrder(status = OrderStatus.PAID)
            every { orderRepository.findById(1L) } returns Optional.of(order)

            val ex = assertThrows<CustomException> {
                paymentService.requestPayment(1L, PaymentRequest(1L, PaymentMethod.CARD))
            }
            assertEquals(ErrorCode.ORDER_NOT_PAYABLE, ex.errorCode)
        }

        @Test
        fun `이미 완료된 결제가 있으면 ORDER_ALREADY_PAID`() {
            val order = createOrder()
            val existing = createPayment(status = PaymentStatus.COMPLETED)
            every { orderRepository.findById(1L) } returns Optional.of(order)
            every { paymentRepository.findByOrderId(1L) } returns existing

            val ex = assertThrows<CustomException> {
                paymentService.requestPayment(1L, PaymentRequest(1L, PaymentMethod.CARD))
            }
            assertEquals(ErrorCode.ORDER_ALREADY_PAID, ex.errorCode)
        }

        @Test
        fun `결제 실패 시뮬레이션에서 상품 옵션 존재하지 않으면 PRODUCT_OPTION_NOT_FOUND`() {
            // given
            val order = createOrder()
            val paymentSlot = slot<Payment>()
            every { orderRepository.findById(1L) } returns Optional.of(order)
            every { paymentRepository.findByOrderId(1L) } returns null
            every { paymentRepository.save(capture(paymentSlot)) } answers { paymentSlot.captured }
            every { productOptionRepository.findByIdWithLock(1L) } returns null

            // when / then
            val ex = assertThrows<CustomException> {
                paymentService.requestPayment(
                    userId = 1L,
                    request = PaymentRequest(orderId = 1L, method = PaymentMethod.CARD, simulateFailure = true),
                )
            }
            assertEquals(ErrorCode.PRODUCT_OPTION_NOT_FOUND, ex.errorCode)
        }
    }

    @Nested
    inner class GetPayment {

        @Test
        fun `본인 결제 내역 조회 성공`() {
            val payment = createPayment(status = PaymentStatus.COMPLETED)
            every { paymentRepository.findByOrderId(1L) } returns payment

            val result = paymentService.getPayment(userId = 1L, orderId = 1L)

            assertEquals(1L, result.orderId)
            assertEquals(PaymentStatus.COMPLETED, result.status)
        }

        @Test
        fun `결제 내역 없으면 PAYMENT_NOT_FOUND`() {
            every { paymentRepository.findByOrderId(999L) } returns null

            val ex = assertThrows<CustomException> { paymentService.getPayment(1L, 999L) }
            assertEquals(ErrorCode.PAYMENT_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `다른 사용자 결제 내역 조회 시 ORDER_NOT_OWNED`() {
            val payment = createPayment(userId = 2L)
            every { paymentRepository.findByOrderId(1L) } returns payment

            val ex = assertThrows<CustomException> { paymentService.getPayment(1L, 1L) }
            assertEquals(ErrorCode.ORDER_NOT_OWNED, ex.errorCode)
        }
    }
}
