package com.example.commerce.order.repository

import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderItem
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.product.entity.Product
import com.example.commerce.product.entity.ProductOption
import com.example.commerce.product.repository.ProductOptionRepository
import com.example.commerce.product.repository.ProductRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest

@DataJpaTest(properties = ["spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"])
class OrderRepositoryTest {

    @Autowired lateinit var orderRepository: OrderRepository
    @Autowired lateinit var orderItemRepository: OrderItemRepository
    @Autowired lateinit var productRepository: ProductRepository
    @Autowired lateinit var productOptionRepository: ProductOptionRepository

    private lateinit var product: Product
    private lateinit var option: ProductOption

    @BeforeEach
    fun setUp() {
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()
        product = productRepository.save(Product(name = "테스트 상품", price = 10000L, categoryId = 1L))
        option = productOptionRepository.save(ProductOption(product = product, size = "M", color = "블랙", stock = 10))
    }

    private fun saveOrder(userId: Long, status: OrderStatus = OrderStatus.PENDING): Order {
        val order = Order(userId = userId, totalAmount = 20000L, status = status)
        order.items.add(
            OrderItem(
                order = order, productId = product.id, productOptionId = option.id,
                productName = product.name, optionInfo = "M / 블랙", price = 10000L, quantity = 2,
            )
        )
        return orderRepository.save(order)
    }

    @Nested
    inner class FindAllByUserId {

        @Test
        fun `userId로 주문 목록 조회 성공`() {
            saveOrder(userId = 1L)
            saveOrder(userId = 1L)
            saveOrder(userId = 2L)

            val result = orderRepository.findAllByUserIdOrderByCreatedAtDesc(1L)

            assertEquals(2, result.size)
            assertTrue(result.all { it.userId == 1L })
        }

        @Test
        fun `주문 없으면 빈 목록 반환`() {
            val result = orderRepository.findAllByUserIdOrderByCreatedAtDesc(999L)

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class OrderItemCascade {

        @Test
        fun `Order 저장 시 OrderItem 함께 저장`() {
            val order = saveOrder(userId = 1L)

            val found = orderRepository.findById(order.id).get()
            assertEquals(1, found.items.size)
            assertEquals("테스트 상품", found.items[0].productName)
            assertEquals("M / 블랙", found.items[0].optionInfo)
        }

        @Test
        fun `Order 삭제 시 OrderItem 함께 삭제`() {
            val order = saveOrder(userId = 1L)

            orderRepository.deleteById(order.id)

            assertEquals(0, orderItemRepository.count())
        }
    }

    @Nested
    inner class StatusUpdate {

        @Test
        fun `주문 상태 변경 정상 반영`() {
            val order = saveOrder(userId = 1L, status = OrderStatus.PENDING)
            order.status = OrderStatus.CANCELLED
            orderRepository.save(order)

            val found = orderRepository.findById(order.id).get()
            assertEquals(OrderStatus.CANCELLED, found.status)
        }
    }
}
