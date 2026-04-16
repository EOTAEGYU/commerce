package com.example.commerce.cart.service

import com.example.commerce.cart.dto.CartItemAddRequest
import com.example.commerce.cart.dto.CartItemUpdateRequest
import com.example.commerce.cart.entity.Cart
import com.example.commerce.cart.entity.CartItem
import com.example.commerce.cart.repository.CartItemRepository
import com.example.commerce.cart.repository.CartRepository
import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.product.entity.Product
import com.example.commerce.product.entity.ProductOption
import com.example.commerce.product.repository.ProductOptionRepository
import com.example.commerce.product.repository.ProductRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Optional

@ExtendWith(MockKExtension::class)
class CartServiceTest {

    @MockK lateinit var cartRepository: CartRepository
    @MockK lateinit var cartItemRepository: CartItemRepository
    @MockK lateinit var productRepository: ProductRepository
    @MockK lateinit var productOptionRepository: ProductOptionRepository

    @InjectMockKs
    lateinit var cartService: CartService

    private fun createProduct(id: Long = 1L) = Product(
        name = "테스트 상품", price = 10000L, categoryId = 1L, id = id,
    )

    private fun createOption(product: Product, stock: Int = 10, id: Long = 1L) =
        ProductOption(product = product, size = "M", color = "블랙", stock = stock, id = id)

    private fun createCart(userId: Long = 1L, id: Long = 1L) = Cart(userId = userId, id = id)

    private fun createCartItem(cart: Cart, optionId: Long = 1L, quantity: Int = 2, id: Long = 1L) =
        CartItem(cart = cart, productId = 1L, productOptionId = optionId, quantity = quantity, price = 10000L, id = id)

    @Nested
    inner class GetCart {

        @Test
        fun `장바구니 존재 시 CartResponse 반환`() {
            val cart = createCart()
            cart.items.add(createCartItem(cart))

            every { cartRepository.findByUserId(1L) } returns cart

            val result = cartService.getCart(1L)

            assertEquals(1L, result.id)
            assertEquals(1, result.items.size)
            assertEquals(20000L, result.totalAmount)
        }

        @Test
        fun `장바구니 없으면 빈 CartResponse 반환`() {
            every { cartRepository.findByUserId(1L) } returns null

            val result = cartService.getCart(1L)

            assertEquals(0L, result.id)
            assertTrue(result.items.isEmpty())
            assertEquals(0L, result.totalAmount)
        }
    }

    @Nested
    inner class AddItem {

        @Test
        fun `새 아이템 추가 성공`() {
            val product = createProduct()
            val option = createOption(product, stock = 10)
            val cart = createCart()
            val request = CartItemAddRequest(productId = 1L, productOptionId = 1L, quantity = 3)

            every { productRepository.findById(1L) } returns Optional.of(product)
            every { productOptionRepository.findById(1L) } returns Optional.of(option)
            every { cartRepository.findByUserId(1L) } returns cart
            every { cartItemRepository.findByCartIdAndProductOptionId(1L, 1L) } returns null

            val result = cartService.addItem(1L, request)

            assertEquals(1, result.items.size)
            assertEquals(3, result.items[0].quantity)
            assertEquals(30000L, result.totalAmount)
        }

        @Test
        fun `같은 옵션 재추가 시 수량 합산`() {
            val product = createProduct()
            val option = createOption(product, stock = 10)
            val cart = createCart()
            val existing = createCartItem(cart, quantity = 2)
            val request = CartItemAddRequest(productId = 1L, productOptionId = 1L, quantity = 3)

            every { productRepository.findById(1L) } returns Optional.of(product)
            every { productOptionRepository.findById(1L) } returns Optional.of(option)
            every { cartRepository.findByUserId(1L) } returns cart
            every { cartItemRepository.findByCartIdAndProductOptionId(1L, 1L) } returns existing

            cartService.addItem(1L, request)

            assertEquals(5, existing.quantity)
        }

        @Test
        fun `재고 부족 시 OUT_OF_STOCK 예외 발생`() {
            val product = createProduct()
            val option = createOption(product, stock = 2)
            val cart = createCart()
            val request = CartItemAddRequest(productId = 1L, productOptionId = 1L, quantity = 5)

            every { productRepository.findById(1L) } returns Optional.of(product)
            every { productOptionRepository.findById(1L) } returns Optional.of(option)
            every { cartRepository.findByUserId(1L) } returns cart
            every { cartItemRepository.findByCartIdAndProductOptionId(1L, 1L) } returns null

            val ex = assertThrows<CustomException> { cartService.addItem(1L, request) }
            assertEquals(ErrorCode.OUT_OF_STOCK, ex.errorCode)
        }

        @Test
        fun `합산 수량이 재고 초과 시 OUT_OF_STOCK 예외 발생`() {
            val product = createProduct()
            val option = createOption(product, stock = 4)
            val cart = createCart()
            val existing = createCartItem(cart, quantity = 3)
            val request = CartItemAddRequest(productId = 1L, productOptionId = 1L, quantity = 3)

            every { productRepository.findById(1L) } returns Optional.of(product)
            every { productOptionRepository.findById(1L) } returns Optional.of(option)
            every { cartRepository.findByUserId(1L) } returns cart
            every { cartItemRepository.findByCartIdAndProductOptionId(1L, 1L) } returns existing

            val ex = assertThrows<CustomException> { cartService.addItem(1L, request) }
            assertEquals(ErrorCode.OUT_OF_STOCK, ex.errorCode)
        }

        @Test
        fun `장바구니 없으면 생성 후 아이템 추가`() {
            val product = createProduct()
            val option = createOption(product, stock = 10)
            val newCart = createCart()
            val request = CartItemAddRequest(productId = 1L, productOptionId = 1L, quantity = 1)

            every { productRepository.findById(1L) } returns Optional.of(product)
            every { productOptionRepository.findById(1L) } returns Optional.of(option)
            every { cartRepository.findByUserId(1L) } returns null
            every { cartRepository.save(any()) } returns newCart
            every { cartItemRepository.findByCartIdAndProductOptionId(1L, 1L) } returns null

            val result = cartService.addItem(1L, request)

            assertEquals(1, result.items.size)
            verify(exactly = 1) { cartRepository.save(any()) }
        }

        @Test
        fun `존재하지 않는 상품 추가 시 PRODUCT_NOT_FOUND 예외 발생`() {
            val request = CartItemAddRequest(productId = 999L, productOptionId = 1L, quantity = 1)

            every { productRepository.findById(999L) } returns Optional.empty()

            val ex = assertThrows<CustomException> { cartService.addItem(1L, request) }
            assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `상품은 존재하고 옵션이 없을 시 PRODUCT_OPTION_NOT_FOUND 예외 발생`() {
            // given
            val product = createProduct()
            val request = CartItemAddRequest(productId = 1L, productOptionId = 999L, quantity = 1)

            every { productRepository.findById(1L) } returns Optional.of(product)
            every { productOptionRepository.findById(999L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> { cartService.addItem(1L, request) }
            assertEquals(ErrorCode.PRODUCT_OPTION_NOT_FOUND, ex.errorCode)
        }
    }

    @Nested
    inner class UpdateItem {

        @Test
        fun `수량 변경 성공`() {
            val cart = createCart()
            val item = createCartItem(cart, quantity = 2)
            val product = createProduct()
            val option = createOption(product, stock = 10)
            val request = CartItemUpdateRequest(quantity = 5)

            every { cartRepository.findByUserId(1L) } returns cart
            every { cartItemRepository.findById(1L) } returns Optional.of(item)
            every { productOptionRepository.findById(1L) } returns Optional.of(option)

            cartService.updateItem(1L, 1L, request)

            assertEquals(5, item.quantity)
        }

        @Test
        fun `재고 부족 시 OUT_OF_STOCK 예외 발생`() {
            val cart = createCart()
            val item = createCartItem(cart, quantity = 2)
            val product = createProduct()
            val option = createOption(product, stock = 3)
            val request = CartItemUpdateRequest(quantity = 5)

            every { cartRepository.findByUserId(1L) } returns cart
            every { cartItemRepository.findById(1L) } returns Optional.of(item)
            every { productOptionRepository.findById(1L) } returns Optional.of(option)

            val ex = assertThrows<CustomException> { cartService.updateItem(1L, 1L, request) }
            assertEquals(ErrorCode.OUT_OF_STOCK, ex.errorCode)
        }

        @Test
        fun `다른 사용자의 아이템 수정 시 CART_ITEM_NOT_OWNED 예외 발생`() {
            val myCart = createCart(userId = 1L, id = 1L)
            val otherCart = createCart(userId = 2L, id = 2L)
            val item = createCartItem(otherCart)
            val request = CartItemUpdateRequest(quantity = 1)

            every { cartRepository.findByUserId(1L) } returns myCart
            every { cartItemRepository.findById(1L) } returns Optional.of(item)

            val ex = assertThrows<CustomException> { cartService.updateItem(1L, 1L, request) }
            assertEquals(ErrorCode.CART_ITEM_NOT_OWNED, ex.errorCode)
        }

        @Test
        fun `장바구니 없는 사용자 수정 시 CART_ITEM_NOT_FOUND 예외 발생`() {
            every { cartRepository.findByUserId(1L) } returns null

            val ex = assertThrows<CustomException> { cartService.updateItem(1L, 1L, CartItemUpdateRequest(quantity = 1)) }
            assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `cartItemRepository findById가 empty 반환 시 CART_ITEM_NOT_FOUND 예외 발생`() {
            // given
            val cart = createCart()

            every { cartRepository.findByUserId(1L) } returns cart
            every { cartItemRepository.findById(999L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> { cartService.updateItem(1L, 999L, CartItemUpdateRequest(quantity = 1)) }
            assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `소유권 확인 통과 후 productOptionRepository findById가 empty 반환 시 PRODUCT_OPTION_NOT_FOUND 예외 발생`() {
            // given
            val cart = createCart(userId = 1L, id = 1L)
            val item = createCartItem(cart, optionId = 999L)

            every { cartRepository.findByUserId(1L) } returns cart
            every { cartItemRepository.findById(1L) } returns Optional.of(item)
            every { productOptionRepository.findById(999L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> { cartService.updateItem(1L, 1L, CartItemUpdateRequest(quantity = 1)) }
            assertEquals(ErrorCode.PRODUCT_OPTION_NOT_FOUND, ex.errorCode)
        }
    }

    @Nested
    inner class DeleteItem {

        @Test
        fun `아이템 삭제 성공`() {
            val cart = createCart()
            val item = createCartItem(cart)
            cart.items.add(item)

            every { cartRepository.findByUserId(1L) } returns cart
            every { cartItemRepository.findById(1L) } returns Optional.of(item)

            val result = cartService.deleteItem(1L, 1L)

            assertTrue(result.items.isEmpty())
        }

        @Test
        fun `다른 사용자의 아이템 삭제 시 CART_ITEM_NOT_OWNED 예외 발생`() {
            val myCart = createCart(userId = 1L, id = 1L)
            val otherCart = createCart(userId = 2L, id = 2L)
            val item = createCartItem(otherCart)

            every { cartRepository.findByUserId(1L) } returns myCart
            every { cartItemRepository.findById(1L) } returns Optional.of(item)

            val ex = assertThrows<CustomException> { cartService.deleteItem(1L, 1L) }
            assertEquals(ErrorCode.CART_ITEM_NOT_OWNED, ex.errorCode)
        }

        @Test
        fun `존재하지 않는 아이템 삭제 시 CART_ITEM_NOT_FOUND 예외 발생`() {
            val cart = createCart()

            every { cartRepository.findByUserId(1L) } returns cart
            every { cartItemRepository.findById(999L) } returns Optional.empty()

            val ex = assertThrows<CustomException> { cartService.deleteItem(1L, 999L) }
            assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `cartRepository findByUserId가 null 반환 시 CART_ITEM_NOT_FOUND 예외 발생`() {
            // given
            every { cartRepository.findByUserId(1L) } returns null

            // when / then
            val ex = assertThrows<CustomException> { cartService.deleteItem(1L, 1L) }
            assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.errorCode)
        }
    }
}
