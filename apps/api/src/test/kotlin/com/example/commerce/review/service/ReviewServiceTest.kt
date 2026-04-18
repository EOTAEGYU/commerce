package com.example.commerce.review.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderItem
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.order.repository.OrderItemRepository
import com.example.commerce.order.repository.OrderRepository
import com.example.commerce.point.service.PointService
import com.example.commerce.review.dto.ReviewCreateRequest
import com.example.commerce.review.dto.ReviewUpdateRequest
import com.example.commerce.review.entity.Review
import com.example.commerce.review.repository.ReviewRepository
import com.example.commerce.user.entity.User
import com.example.commerce.user.repository.UserRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

@ExtendWith(MockKExtension::class)
class ReviewServiceTest {

    @MockK lateinit var reviewRepository: ReviewRepository
    @MockK lateinit var orderItemRepository: OrderItemRepository
    @MockK lateinit var orderRepository: OrderRepository
    @MockK lateinit var userRepository: UserRepository
    @MockK lateinit var pointService: PointService

    @InjectMockKs
    lateinit var reviewService: ReviewService

    private fun createOrder(
        userId: Long = 1L,
        status: OrderStatus = OrderStatus.DELIVERED,
        id: Long = 1L,
    ) = Order(userId = userId, totalAmount = 10000L, status = status, id = id)

    private fun createOrderItem(
        order: Order,
        productId: Long = 10L,
        id: Long = 1L,
    ) = OrderItem(
        order = order,
        productId = productId,
        productOptionId = 1L,
        productName = "테스트 상품",
        optionInfo = "M / 블랙",
        price = 10000L,
        quantity = 1,
        id = id,
    )

    private fun createReview(
        userId: Long = 1L,
        productId: Long = 10L,
        orderId: Long = 1L,
        orderItemId: Long = 1L,
        rating: Double = 4.5,
        content: String = "좋은 상품입니다.",
        id: Long = 1L,
    ) = Review(
        userId = userId,
        productId = productId,
        orderId = orderId,
        orderItemId = orderItemId,
        rating = rating,
        content = content,
        id = id,
    )

    private fun createUser(userId: Long = 1L, name: String = "홍길동") =
        User(email = "test@example.com", password = "password", name = name, id = userId)

    @Nested
    inner class CreateReview {

        @Test
        fun `OrderItem이 존재하지 않을 시 ORDER_ITEM_NOT_FOUND 예외 발생`() {
            // given
            every { orderItemRepository.findById(99L) } returns Optional.empty()

            val request = ReviewCreateRequest(orderItemId = 99L, rating = 4.5, content = "좋아요")

            // when / then
            val ex = assertThrows<CustomException> { reviewService.createReview(1L, request) }
            assertEquals(ErrorCode.ORDER_ITEM_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `주문 소유자가 아닐 시 ORDER_NOT_OWNED 예외 발생`() {
            // given
            val order = createOrder(userId = 2L)
            val orderItem = createOrderItem(order)

            every { orderItemRepository.findById(1L) } returns Optional.of(orderItem)
            every { orderRepository.findById(order.id) } returns Optional.of(order)

            val request = ReviewCreateRequest(orderItemId = 1L, rating = 4.5, content = "좋아요")

            // when / then
            val ex = assertThrows<CustomException> { reviewService.createReview(userId = 1L, request = request) }
            assertEquals(ErrorCode.ORDER_NOT_OWNED, ex.errorCode)
        }

        @Test
        fun `주문 상태가 DELIVERED가 아닐 시 ORDER_NOT_DELIVERED 예외 발생`() {
            // given
            val order = createOrder(userId = 1L, status = OrderStatus.PAID)
            val orderItem = createOrderItem(order)

            every { orderItemRepository.findById(1L) } returns Optional.of(orderItem)
            every { orderRepository.findById(order.id) } returns Optional.of(order)

            val request = ReviewCreateRequest(orderItemId = 1L, rating = 4.5, content = "좋아요")

            // when / then
            val ex = assertThrows<CustomException> { reviewService.createReview(userId = 1L, request = request) }
            assertEquals(ErrorCode.ORDER_NOT_DELIVERED, ex.errorCode)
        }

        @Test
        fun `이미 리뷰가 존재할 시 REVIEW_ALREADY_EXISTS 예외 발생`() {
            // given
            val order = createOrder(userId = 1L, status = OrderStatus.DELIVERED)
            val orderItem = createOrderItem(order)

            every { orderItemRepository.findById(1L) } returns Optional.of(orderItem)
            every { orderRepository.findById(order.id) } returns Optional.of(order)
            every { reviewRepository.existsByOrderItemId(1L) } returns true

            val request = ReviewCreateRequest(orderItemId = 1L, rating = 4.5, content = "좋아요")

            // when / then
            val ex = assertThrows<CustomException> { reviewService.createReview(userId = 1L, request = request) }
            assertEquals(ErrorCode.REVIEW_ALREADY_EXISTS, ex.errorCode)
        }

        @Test
        fun `별점이 0_5 단위가 아닐 시 INVALID_RATING 예외 발생`() {
            // given
            val order = createOrder(userId = 1L, status = OrderStatus.DELIVERED)
            val orderItem = createOrderItem(order)

            every { orderItemRepository.findById(1L) } returns Optional.of(orderItem)
            every { orderRepository.findById(order.id) } returns Optional.of(order)
            every { reviewRepository.existsByOrderItemId(1L) } returns false

            val request = ReviewCreateRequest(orderItemId = 1L, rating = 1.3, content = "좋아요")

            // when / then
            val ex = assertThrows<CustomException> { reviewService.createReview(userId = 1L, request = request) }
            assertEquals(ErrorCode.INVALID_RATING, ex.errorCode)
        }

        @Test
        fun `정상 입력으로 호출 시 리뷰 저장 후 ReviewResponse 반환`() {
            // given
            val order = createOrder(userId = 1L, status = OrderStatus.DELIVERED)
            val orderItem = createOrderItem(order)
            val user = createUser()
            val reviewSlot = slot<Review>()
            val savedReview = createReview()

            every { orderItemRepository.findById(1L) } returns Optional.of(orderItem)
            every { orderRepository.findById(order.id) } returns Optional.of(order)
            every { reviewRepository.existsByOrderItemId(1L) } returns false
            every { reviewRepository.save(capture(reviewSlot)) } returns savedReview
            every { pointService.earnReviewPoints(1L, savedReview.id) } just runs
            every { userRepository.findById(1L) } returns Optional.of(user)

            val request = ReviewCreateRequest(orderItemId = 1L, rating = 4.5, content = "좋은 상품입니다.")

            // when
            val result = reviewService.createReview(userId = 1L, request = request)

            // then
            assertEquals(savedReview.id, result.id)
            assertEquals(4.5, result.rating)
            assertEquals("좋은 상품입니다.", result.content)
            assertEquals("홍길동", result.userName)
            verify(exactly = 1) { reviewRepository.save(any()) }
        }

        @Test
        fun `정상 리뷰 생성 시 userId와 productId가 올바르게 설정됨`() {
            // given
            val order = createOrder(userId = 1L, status = OrderStatus.DELIVERED)
            val orderItem = createOrderItem(order, productId = 10L)
            val user = createUser()
            val savedReview = createReview(userId = 1L, productId = 10L)

            every { orderItemRepository.findById(1L) } returns Optional.of(orderItem)
            every { orderRepository.findById(order.id) } returns Optional.of(order)
            every { reviewRepository.existsByOrderItemId(1L) } returns false
            every { reviewRepository.save(any()) } returns savedReview
            every { pointService.earnReviewPoints(1L, savedReview.id) } just runs
            every { userRepository.findById(1L) } returns Optional.of(user)

            val request = ReviewCreateRequest(orderItemId = 1L, rating = 4.5, content = "좋은 상품입니다.")

            // when
            val result = reviewService.createReview(userId = 1L, request = request)

            // then
            assertEquals(1L, result.userId)
            assertEquals(10L, result.productId)
        }
    }

    @Nested
    inner class UpdateReview {

        @Test
        fun `리뷰가 존재하지 않을 시 REVIEW_NOT_FOUND 예외 발생`() {
            // given
            every { reviewRepository.findById(99L) } returns Optional.empty()

            val request = ReviewUpdateRequest(rating = 3.5, content = "수정된 내용")

            // when / then
            val ex = assertThrows<CustomException> { reviewService.updateReview(userId = 1L, reviewId = 99L, request = request) }
            assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `리뷰 소유자가 아닐 시 REVIEW_NOT_OWNED 예외 발생`() {
            // given
            val review = createReview(userId = 2L)
            every { reviewRepository.findById(1L) } returns Optional.of(review)

            val request = ReviewUpdateRequest(rating = 3.5, content = "수정된 내용")

            // when / then
            val ex = assertThrows<CustomException> { reviewService.updateReview(userId = 1L, reviewId = 1L, request = request) }
            assertEquals(ErrorCode.REVIEW_NOT_OWNED, ex.errorCode)
        }

        @Test
        fun `별점이 0_5 단위가 아닐 시 INVALID_RATING 예외 발생`() {
            // given
            val review = createReview(userId = 1L)
            every { reviewRepository.findById(1L) } returns Optional.of(review)

            val request = ReviewUpdateRequest(rating = 2.7, content = "수정된 내용")

            // when / then
            val ex = assertThrows<CustomException> { reviewService.updateReview(userId = 1L, reviewId = 1L, request = request) }
            assertEquals(ErrorCode.INVALID_RATING, ex.errorCode)
        }

        @Test
        fun `정상 수정 시 content와 rating이 업데이트된 ReviewResponse 반환`() {
            // given
            val review = createReview(userId = 1L, rating = 4.5, content = "원래 내용")
            val user = createUser()

            every { reviewRepository.findById(1L) } returns Optional.of(review)
            every { userRepository.findById(1L) } returns Optional.of(user)

            val request = ReviewUpdateRequest(rating = 3.5, content = "수정된 내용")

            // when
            val result = reviewService.updateReview(userId = 1L, reviewId = 1L, request = request)

            // then
            assertEquals(3.5, result.rating)
            assertEquals("수정된 내용", result.content)
            assertEquals("홍길동", result.userName)
        }

        @Test
        fun `정상 수정 시 reviewRepository save를 별도 호출하지 않음 (dirty checking)`() {
            // given
            val review = createReview(userId = 1L)
            val user = createUser()

            every { reviewRepository.findById(1L) } returns Optional.of(review)
            every { userRepository.findById(1L) } returns Optional.of(user)

            val request = ReviewUpdateRequest(rating = 3.5, content = "수정된 내용")

            // when
            reviewService.updateReview(userId = 1L, reviewId = 1L, request = request)

            // then
            verify(exactly = 0) { reviewRepository.save(any()) }
        }
    }

    @Nested
    inner class DeleteReview {

        @Test
        fun `리뷰가 존재하지 않을 시 REVIEW_NOT_FOUND 예외 발생`() {
            // given
            every { reviewRepository.findById(99L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> { reviewService.deleteReview(userId = 1L, reviewId = 99L) }
            assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `리뷰 소유자가 아닐 시 REVIEW_NOT_OWNED 예외 발생`() {
            // given
            val review = createReview(userId = 2L)
            every { reviewRepository.findById(1L) } returns Optional.of(review)

            // when / then
            val ex = assertThrows<CustomException> { reviewService.deleteReview(userId = 1L, reviewId = 1L) }
            assertEquals(ErrorCode.REVIEW_NOT_OWNED, ex.errorCode)
        }

        @Test
        fun `정상 삭제 시 reviewRepository delete 1회 호출`() {
            // given
            val review = createReview(userId = 1L)
            every { reviewRepository.findById(1L) } returns Optional.of(review)
            every { reviewRepository.delete(review) } returns Unit

            // when
            reviewService.deleteReview(userId = 1L, reviewId = 1L)

            // then
            verify(exactly = 1) { reviewRepository.delete(review) }
        }
    }

    @Nested
    inner class GetProductReviews {

        @Test
        fun `상품별 리뷰 목록을 페이지 단위로 조회하여 ReviewResponse 목록 반환`() {
            // given
            val reviews = listOf(
                createReview(id = 1L, rating = 5.0, content = "최고"),
                createReview(id = 2L, rating = 4.0, content = "좋아요"),
            )
            val pageable = PageRequest.of(0, 10)
            val page = PageImpl(reviews, pageable, reviews.size.toLong())

            every { reviewRepository.findAllByProductIdOrderByCreatedAtDesc(10L, pageable) } returns page

            // when
            val result = reviewService.getProductReviews(productId = 10L, pageable = pageable)

            // then
            assertEquals(2, result.totalElements)
            assertEquals(5.0, result.content[0].rating)
            assertEquals(4.0, result.content[1].rating)
        }

        @Test
        fun `리뷰가 없을 시 빈 페이지 반환`() {
            // given
            val pageable = PageRequest.of(0, 10)
            val emptyPage = PageImpl<Review>(emptyList(), pageable, 0)

            every { reviewRepository.findAllByProductIdOrderByCreatedAtDesc(10L, pageable) } returns emptyPage

            // when
            val result = reviewService.getProductReviews(productId = 10L, pageable = pageable)

            // then
            assertTrue(result.isEmpty)
            assertEquals(0, result.totalElements)
        }
    }

    @Nested
    inner class GetMyReviews {

        @Test
        fun `내 리뷰 목록 조회 시 userName 포함한 ReviewResponse 목록 반환`() {
            // given
            val user = createUser(userId = 1L, name = "홍길동")
            val reviews = listOf(
                createReview(userId = 1L, id = 1L, rating = 5.0, content = "첫 번째 리뷰"),
                createReview(userId = 1L, id = 2L, rating = 3.5, content = "두 번째 리뷰"),
            )

            every { userRepository.findById(1L) } returns Optional.of(user)
            every { reviewRepository.findAllByUserIdOrderByCreatedAtDesc(1L) } returns reviews

            // when
            val result = reviewService.getMyReviews(userId = 1L)

            // then
            assertEquals(2, result.size)
            assertEquals("홍길동", result[0].userName)
            assertEquals("홍길동", result[1].userName)
            assertEquals(5.0, result[0].rating)
            assertEquals(3.5, result[1].rating)
        }

        @Test
        fun `리뷰가 없을 시 빈 목록 반환`() {
            // given
            val user = createUser()
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { reviewRepository.findAllByUserIdOrderByCreatedAtDesc(1L) } returns emptyList()

            // when
            val result = reviewService.getMyReviews(userId = 1L)

            // then
            assertTrue(result.isEmpty())
        }

        @Test
        fun `userRepository에서 사용자를 찾지 못할 경우 userName이 null인 ReviewResponse 반환`() {
            // given
            val reviews = listOf(createReview(userId = 1L))
            every { userRepository.findById(1L) } returns Optional.empty()
            every { reviewRepository.findAllByUserIdOrderByCreatedAtDesc(1L) } returns reviews

            // when
            val result = reviewService.getMyReviews(userId = 1L)

            // then
            assertEquals(1, result.size)
            assertEquals(null, result[0].userName)
        }
    }
}
