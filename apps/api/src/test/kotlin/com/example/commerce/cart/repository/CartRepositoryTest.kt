package com.example.commerce.cart.repository

import com.example.commerce.cart.entity.Cart
import com.example.commerce.cart.entity.CartItem
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
class CartRepositoryTest {

    @Autowired lateinit var cartRepository: CartRepository
    @Autowired lateinit var cartItemRepository: CartItemRepository
    @Autowired lateinit var productRepository: ProductRepository
    @Autowired lateinit var productOptionRepository: ProductOptionRepository

    private lateinit var product: Product
    private lateinit var option: ProductOption

    @BeforeEach
    fun setUp() {
        cartItemRepository.deleteAll()
        cartRepository.deleteAll()
        product = productRepository.save(Product(name = "테스트 상품", price = 10000L, categoryId = 1L))
        option = productOptionRepository.save(ProductOption(product = product, size = "M", color = "블랙", stock = 10))
    }

    @Nested
    inner class FindByUserId {

        @Test
        fun `userId로 장바구니 조회 성공`() {
            cartRepository.save(Cart(userId = 1L))

            val result = cartRepository.findByUserId(1L)

            assertNotNull(result)
            assertEquals(1L, result!!.userId)
        }

        @Test
        fun `존재하지 않는 userId 조회 시 null 반환`() {
            val result = cartRepository.findByUserId(999L)

            assertNull(result)
        }
    }

    @Nested
    inner class CartItemCascade {

        @Test
        fun `Cart 저장 시 CartItem 함께 저장`() {
            val cart = cartRepository.save(Cart(userId = 1L))
            cart.items.add(CartItem(cart = cart, productId = product.id, productOptionId = option.id, quantity = 2, price = 10000L))
            cartRepository.save(cart)

            val found = cartRepository.findByUserId(1L)!!
            assertEquals(1, found.items.size)
            assertEquals(2, found.items[0].quantity)
        }

        @Test
        fun `Cart 삭제 시 CartItem 함께 삭제`() {
            val cart = cartRepository.save(Cart(userId = 1L))
            cart.items.add(CartItem(cart = cart, productId = product.id, productOptionId = option.id, quantity = 1, price = 10000L))
            cartRepository.save(cart)

            cartRepository.deleteById(cart.id)

            assertEquals(0, cartItemRepository.count())
        }
    }

    @Nested
    inner class FindByCartIdAndProductOptionId {

        @Test
        fun `cartId와 optionId로 CartItem 조회 성공`() {
            val cart = cartRepository.save(Cart(userId = 1L))
            cart.items.add(CartItem(cart = cart, productId = product.id, productOptionId = option.id, quantity = 3, price = 10000L))
            cartRepository.save(cart)

            val result = cartItemRepository.findByCartIdAndProductOptionId(cart.id, option.id)

            assertNotNull(result)
            assertEquals(3, result!!.quantity)
        }

        @Test
        fun `없는 optionId 조회 시 null 반환`() {
            val cart = cartRepository.save(Cart(userId = 1L))

            val result = cartItemRepository.findByCartIdAndProductOptionId(cart.id, 999L)

            assertNull(result)
        }
    }
}
