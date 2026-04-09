package com.example.commerce.order.scheduler

import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.order.repository.OrderRepository
import com.example.commerce.product.repository.ProductOptionRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class OrderExpirationScheduler(
    private val orderRepository: OrderRepository,
    private val productOptionRepository: ProductOptionRepository,
) {
    @Scheduled(fixedRate = 60_000)
    fun expireOrders() {
        val expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
            OrderStatus.PENDING,
            LocalDateTime.now().minusMinutes(10),
        )
        expiredOrders.forEach { expireSingleOrder(it) }
    }

    @Transactional
    fun expireSingleOrder(order: Order) {
        order.items.forEach { item ->
            val option = productOptionRepository.findByIdWithLock(item.productOptionId) ?: return@forEach
            option.stock += item.quantity
        }
        order.status = OrderStatus.CANCELLED
    }
}
