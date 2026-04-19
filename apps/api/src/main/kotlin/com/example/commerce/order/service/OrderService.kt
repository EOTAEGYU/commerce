package com.example.commerce.order.service

import com.example.commerce.cart.repository.CartRepository
import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.order.dto.OrderResponse
import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderItem
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.order.repository.OrderRepository
import com.example.commerce.product.repository.ProductOptionRepository
import com.example.commerce.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val productOptionRepository: ProductOptionRepository,
) {
    fun createOrder(userId: Long): OrderResponse {
        val cart = cartRepository.findByUserId(userId)
        if (cart == null || cart.items.isEmpty()) throw CustomException(ErrorCode.CART_EMPTY)

        // 재고 확인 및 스냅샷 데이터 수집
        data class ItemSnapshot(
            val productId: Long, val productOptionId: Long,
            val productName: String, val optionInfo: String,
            val price: Long, val quantity: Int,
        )

        val snapshots = cart.items.map { cartItem ->
            val option = productOptionRepository.findByIdWithLock(cartItem.productOptionId)
                ?: throw CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND)
            if (option.stock < cartItem.quantity) throw CustomException(ErrorCode.OUT_OF_STOCK)

            val product = productRepository.findById(cartItem.productId)
                .orElseThrow { CustomException(ErrorCode.PRODUCT_NOT_FOUND) }

            option.stock -= cartItem.quantity

            ItemSnapshot(
                productId = product.id,
                productOptionId = option.id,
                productName = product.name,
                optionInfo = "${option.size} / ${option.color}",
                price = cartItem.price,
                quantity = cartItem.quantity,
            )
        }

        val totalAmount = snapshots.sumOf { it.price * it.quantity }
        val order = Order(userId = userId, totalAmount = totalAmount)

        snapshots.forEach { s ->
            order.items.add(
                OrderItem(
                    order = order,
                    productId = s.productId,
                    productOptionId = s.productOptionId,
                    productName = s.productName,
                    optionInfo = s.optionInfo,
                    price = s.price,
                    quantity = s.quantity,
                )
            )
        }

        val savedOrder = orderRepository.save(order)
        cart.items.clear()

        return OrderResponse.from(savedOrder)
    }

    @Transactional(readOnly = true)
    fun getOrder(userId: Long, orderId: Long): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { CustomException(ErrorCode.ORDER_NOT_FOUND) }
        if (order.userId != userId) throw CustomException(ErrorCode.ORDER_NOT_OWNED)
        return OrderResponse.from(order)
    }

    @Transactional(readOnly = true)
    fun getOrders(userId: Long): List<OrderResponse> =
        orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map { OrderResponse.from(it) }

    fun cancelOrder(userId: Long, orderId: Long): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { CustomException(ErrorCode.ORDER_NOT_FOUND) }
        if (order.userId != userId) throw CustomException(ErrorCode.ORDER_NOT_OWNED)
        if (order.status !in listOf(OrderStatus.PENDING, OrderStatus.PAID))
            throw CustomException(ErrorCode.ORDER_CANNOT_CANCEL)

        order.items.forEach { item ->
            val option = productOptionRepository.findByIdWithLock(item.productOptionId)
                ?: throw CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND)
            option.stock += item.quantity
        }

        order.status = OrderStatus.CANCELLED
        return OrderResponse.from(order)
    }
}
