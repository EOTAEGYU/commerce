package com.example.commerce.cart.service

import com.example.commerce.cart.dto.CartItemAddRequest
import com.example.commerce.cart.dto.CartItemUpdateRequest
import com.example.commerce.cart.dto.CartResponse
import com.example.commerce.cart.entity.Cart
import com.example.commerce.cart.entity.CartItem
import com.example.commerce.cart.repository.CartItemRepository
import com.example.commerce.cart.repository.CartRepository
import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.product.repository.ProductOptionRepository
import com.example.commerce.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CartService(
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
    private val productOptionRepository: ProductOptionRepository,
) {
    @Transactional(readOnly = true)
    fun getCart(userId: Long): CartResponse {
        val cart = cartRepository.findByUserId(userId)
            ?: return CartResponse(id = 0, userId = userId, items = emptyList(), totalAmount = 0L)
        return CartResponse.from(cart)
    }

    fun addItem(userId: Long, request: CartItemAddRequest): CartResponse {
        val product = productRepository.findById(request.productId)
            .orElseThrow { CustomException(ErrorCode.PRODUCT_NOT_FOUND) }
        val option = productOptionRepository.findById(request.productOptionId)
            .orElseThrow { CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND) }

        val cart = cartRepository.findByUserId(userId) ?: cartRepository.save(Cart(userId = userId))
        val existing = cartItemRepository.findByCartIdAndProductOptionId(cart.id, request.productOptionId)

        if (existing != null) {
            val newQuantity = existing.quantity + request.quantity
            if (option.stock < newQuantity)
                throw CustomException(ErrorCode.OUT_OF_STOCK)
            existing.quantity = newQuantity
        } else {
            if (option.stock < request.quantity)
                throw CustomException(ErrorCode.OUT_OF_STOCK)
            cart.items.add(
                CartItem(
                    cart = cart,
                    productId = request.productId,
                    productOptionId = request.productOptionId,
                    quantity = request.quantity,
                    price = product.price,
                )
            )
        }

        return CartResponse.from(cart)
    }

    fun updateItem(userId: Long, cartItemId: Long, request: CartItemUpdateRequest): CartResponse {
        val cart = cartRepository.findByUserId(userId)
            ?: throw CustomException(ErrorCode.CART_ITEM_NOT_FOUND)
        val item = cartItemRepository.findById(cartItemId)
            .orElseThrow { CustomException(ErrorCode.CART_ITEM_NOT_FOUND) }
        if (item.cart.id != cart.id)
            throw CustomException(ErrorCode.CART_ITEM_NOT_OWNED)

        val option = productOptionRepository.findById(item.productOptionId)
            .orElseThrow { CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND) }
        if (option.stock < request.quantity)
            throw CustomException(ErrorCode.OUT_OF_STOCK)

        item.quantity = request.quantity
        return CartResponse.from(cart)
    }

    fun deleteItem(userId: Long, cartItemId: Long): CartResponse {
        val cart = cartRepository.findByUserId(userId)
            ?: throw CustomException(ErrorCode.CART_ITEM_NOT_FOUND)
        val item = cartItemRepository.findById(cartItemId)
            .orElseThrow { CustomException(ErrorCode.CART_ITEM_NOT_FOUND) }
        if (item.cart.id != cart.id)
            throw CustomException(ErrorCode.CART_ITEM_NOT_OWNED)

        cart.items.remove(item)
        return CartResponse.from(cart)
    }
}
