package com.example.commerce.order.scheduler

import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderItem
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.order.repository.OrderRepository
import com.example.commerce.product.entity.Product
import com.example.commerce.product.entity.ProductOption
import com.example.commerce.product.repository.ProductOptionRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class OrderExpirationSchedulerTest {

    @MockK lateinit var orderRepository: OrderRepository
    @MockK lateinit var productOptionRepository: ProductOptionRepository

    @InjectMockKs
    lateinit var scheduler: OrderExpirationScheduler

    private fun createExpiredOrder(id: Long = 1L): Order {
        val product = Product(name = "테스트", price = 10000L, categoryId = 1L, id = 1L)
        val option = ProductOption(product = product, size = "M", color = "블랙", stock = 3, id = 1L)
        val order = Order(userId = 1L, totalAmount = 20000L, status = OrderStatus.PENDING, id = id)
        order.items.add(
            OrderItem(
                order = order, productId = product.id, productOptionId = option.id,
                productName = product.name, optionInfo = "M / 블랙", price = 10000L, quantity = 2,
            )
        )
        return order
    }

    @Nested
    inner class ExpireOrders {

        @Test
        fun `만료된 주문이 있으면 expireSingleOrder 호출`() {
            val expiredOrder = createExpiredOrder()
            every {
                orderRepository.findByStatusAndCreatedAtBefore(any(), any())
            } returns listOf(expiredOrder)

            val product = Product(name = "테스트", price = 10000L, categoryId = 1L, id = 1L)
            val option = ProductOption(product = product, size = "M", color = "블랙", stock = 3, id = 1L)
            every { productOptionRepository.findByIdWithLock(1L) } returns option

            scheduler.expireOrders()

            assertEquals(OrderStatus.CANCELLED, expiredOrder.status)
            assertEquals(5, option.stock) // 3 + 2
        }

        @Test
        fun `만료된 주문이 없으면 아무 처리 없음`() {
            every {
                orderRepository.findByStatusAndCreatedAtBefore(any(), any())
            } returns emptyList()

            scheduler.expireOrders()

            verify(exactly = 0) { productOptionRepository.findByIdWithLock(any()) }
        }
    }

    @Nested
    inner class ExpireSingleOrder {

        @Test
        fun `단일 주문 만료 시 재고 복원 및 CANCELLED 처리`() {
            val order = createExpiredOrder()
            val product = Product(name = "테스트", price = 10000L, categoryId = 1L, id = 1L)
            val option = ProductOption(product = product, size = "M", color = "블랙", stock = 8, id = 1L)
            every { productOptionRepository.findByIdWithLock(1L) } returns option

            scheduler.expireSingleOrder(order)

            assertEquals(OrderStatus.CANCELLED, order.status)
            assertEquals(10, option.stock) // 8 + 2
        }

        @Test
        fun `옵션 조회 실패 시 해당 아이템 건너뜀`() {
            val order = createExpiredOrder()
            every { productOptionRepository.findByIdWithLock(1L) } returns null

            scheduler.expireSingleOrder(order)

            assertEquals(OrderStatus.CANCELLED, order.status)
        }
    }
}
