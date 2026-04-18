package com.example.commerce.point.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.order.entity.Order
import com.example.commerce.point.entity.PointHistory
import com.example.commerce.point.entity.PointHistoryType
import com.example.commerce.point.entity.UserPoint
import com.example.commerce.point.repository.PointHistoryRepository
import com.example.commerce.point.repository.UserPointRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@ExtendWith(MockKExtension::class)
class PointServiceTest {

    @MockK lateinit var userPointRepository: UserPointRepository
    @MockK lateinit var pointHistoryRepository: PointHistoryRepository

    @InjectMockKs
    lateinit var pointService: PointService

    private fun createUserPoint(
        userId: Long = 1L,
        balance: Long = 5000L,
        id: Long = 1L,
    ) = UserPoint(userId = userId, balance = balance, id = id)

    private fun createPointHistory(
        userId: Long = 1L,
        type: PointHistoryType = PointHistoryType.USE_PAYMENT,
        amount: Long = 1000L,
        balance: Long = 4000L,
        relatedId: Long? = 10L,
        id: Long = 1L,
    ) = PointHistory(
        userId = userId,
        type = type,
        amount = amount,
        balance = balance,
        relatedId = relatedId,
        id = id,
    )

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class GetBalance {

        @Test
        fun `UserPoint 없을 때 자동 생성 후 잔액 반환`() {
            // given
            val newPoint = createUserPoint(balance = 0L)
            every { userPointRepository.findByUserId(1L) } returns null
            every { userPointRepository.save(any()) } returns newPoint

            // when
            val result = pointService.getBalance(userId = 1L)

            // then
            assertEquals(0L, result.balance)
            verify(exactly = 1) { userPointRepository.save(any()) }
        }

        @Test
        fun `UserPoint 있을 때 기존 잔액 반환`() {
            // given
            val userPoint = createUserPoint(balance = 5000L)
            every { userPointRepository.findByUserId(1L) } returns userPoint

            // when
            val result = pointService.getBalance(userId = 1L)

            // then
            assertEquals(5000L, result.balance)
            verify(exactly = 0) { userPointRepository.save(any()) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class GetHistory {

        @Test
        fun `포인트 이력 페이징 조회 정상 반환`() {
            // given
            val pageable = PageRequest.of(0, 20)
            val history = createPointHistory(type = PointHistoryType.EARN_REVIEW, amount = 300L, balance = 300L)
            val page: Page<PointHistory> = PageImpl(listOf(history), pageable, 1)
            every { pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable) } returns page

            // when
            val result = pointService.getHistory(userId = 1L, pageable = pageable)

            // then
            assertEquals(1, result.totalElements)
            assertEquals(PointHistoryType.EARN_REVIEW, result.content[0].type)
            assertEquals(300L, result.content[0].amount)
        }

        @Test
        fun `포인트 이력 없을 때 빈 페이지 반환`() {
            // given
            val pageable = PageRequest.of(0, 20)
            val emptyPage: Page<PointHistory> = PageImpl(emptyList(), pageable, 0)
            every { pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable) } returns emptyPage

            // when
            val result = pointService.getHistory(userId = 1L, pageable = pageable)

            // then
            assertEquals(0, result.totalElements)
            assertEquals(true, result.isEmpty)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class EarnPoints {

        @Test
        fun `amount 0 이하이면 아무것도 하지 않음`() {
            // when
            pointService.earnPoints(userId = 1L, type = PointHistoryType.EARN_REVIEW, amount = 0L)

            // then
            verify(exactly = 0) { userPointRepository.findByUserId(any()) }
            verify(exactly = 0) { pointHistoryRepository.save(any()) }
        }

        @Test
        fun `amount 음수이면 아무것도 하지 않음`() {
            // when
            pointService.earnPoints(userId = 1L, type = PointHistoryType.EARN_REVIEW, amount = -100L)

            // then
            verify(exactly = 0) { userPointRepository.findByUserId(any()) }
            verify(exactly = 0) { pointHistoryRepository.save(any()) }
        }

        @Test
        fun `정상 적립 시 balance 증가 및 이력 저장`() {
            // given
            val userPoint = createUserPoint(balance = 1000L)
            val savedHistory = createPointHistory(type = PointHistoryType.EARN_REVIEW, amount = 300L, balance = 1300L)
            every { userPointRepository.findByUserId(1L) } returns userPoint
            every { pointHistoryRepository.save(any()) } returns savedHistory

            // when
            pointService.earnPoints(
                userId = 1L,
                type = PointHistoryType.EARN_REVIEW,
                amount = 300L,
                relatedId = 5L,
                description = "리뷰 작성 적립",
            )

            // then
            assertEquals(1300L, userPoint.balance)
            verify(exactly = 1) { pointHistoryRepository.save(any()) }
        }

        @Test
        fun `UserPoint 없을 때 자동 생성 후 적립`() {
            // given
            val newPoint = createUserPoint(balance = 0L)
            every { userPointRepository.findByUserId(1L) } returns null
            every { userPointRepository.save(any()) } returns newPoint
            every { pointHistoryRepository.save(any()) } returns createPointHistory(amount = 500L, balance = 500L)

            // when
            pointService.earnPoints(userId = 1L, type = PointHistoryType.EARN_PURCHASE, amount = 500L)

            // then
            verify(exactly = 1) { userPointRepository.save(any()) }
            verify(exactly = 1) { pointHistoryRepository.save(any()) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class EarnReviewPoints {

        @Test
        fun `리뷰 작성 시 300포인트 EARN_REVIEW 타입으로 적립`() {
            // given
            val userPoint = createUserPoint(balance = 0L)
            every { userPointRepository.findByUserId(1L) } returns userPoint
            every { pointHistoryRepository.save(any()) } returns createPointHistory(
                type = PointHistoryType.EARN_REVIEW, amount = 300L, balance = 300L
            )

            // when
            pointService.earnReviewPoints(userId = 1L, reviewId = 7L)

            // then
            assertEquals(300L, userPoint.balance)
            verify(exactly = 1) { pointHistoryRepository.save(any()) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class EarnPurchasePoints {

        @Test
        fun `구매 적립 시 결제금액의 1% 소수점 버림 후 적립`() {
            // given
            val userPoint = createUserPoint(balance = 0L)
            every { userPointRepository.findByUserId(1L) } returns userPoint
            every { pointHistoryRepository.save(any()) } returns createPointHistory(
                type = PointHistoryType.EARN_PURCHASE, amount = 199L, balance = 199L
            )

            // when — paymentAmount = 19999 → 19999 / 100 = 199 (소수점 버림)
            pointService.earnPurchasePoints(userId = 1L, orderId = 10L, paymentAmount = 19999L)

            // then
            assertEquals(199L, userPoint.balance)
            verify(exactly = 1) { pointHistoryRepository.save(any()) }
        }

        @Test
        fun `구매 적립 - 정확히 나누어 떨어지는 금액`() {
            // given
            val userPoint = createUserPoint(balance = 500L)
            every { userPointRepository.findByUserId(1L) } returns userPoint
            every { pointHistoryRepository.save(any()) } returns createPointHistory(
                type = PointHistoryType.EARN_PURCHASE, amount = 200L, balance = 700L
            )

            // when — paymentAmount = 20000 → 20000 / 100 = 200
            pointService.earnPurchasePoints(userId = 1L, orderId = 10L, paymentAmount = 20000L)

            // then
            assertEquals(700L, userPoint.balance)
            verify(exactly = 1) { pointHistoryRepository.save(any()) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class ValidateAndUsePoints {

        @Test
        fun `pointAmount 0 이하이면 0L 반환하고 아무것도 하지 않음`() {
            // given
            val order = mockk<Order>()

            // when
            val result = pointService.validateAndUsePoints(userId = 1L, pointAmount = 0L, order = order)

            // then
            assertEquals(0L, result)
            verify(exactly = 0) { userPointRepository.findByUserId(any()) }
            verify(exactly = 0) { pointHistoryRepository.save(any()) }
        }

        @Test
        fun `pointAmount 음수이면 0L 반환하고 아무것도 하지 않음`() {
            // given
            val order = mockk<Order>()

            // when
            val result = pointService.validateAndUsePoints(userId = 1L, pointAmount = -500L, order = order)

            // then
            assertEquals(0L, result)
            verify(exactly = 0) { userPointRepository.findByUserId(any()) }
        }

        @Test
        fun `pointAmount 1000 미만이면 POINT_BELOW_MINIMUM 예외 발생`() {
            // given
            val order = mockk<Order>()

            // when / then
            val ex = assertThrows<CustomException> {
                pointService.validateAndUsePoints(userId = 1L, pointAmount = 999L, order = order)
            }
            assertEquals(ErrorCode.POINT_BELOW_MINIMUM, ex.errorCode)
        }

        @Test
        fun `pointAmount가 주문금액의 50% 초과 시 POINT_EXCEEDS_MAXIMUM 예외 발생`() {
            // given
            val order = mockk<Order>()
            every { order.totalAmount } returns 10000L  // maxUsable = 5000

            // when / then — 5001 > 5000
            val ex = assertThrows<CustomException> {
                pointService.validateAndUsePoints(userId = 1L, pointAmount = 5001L, order = order)
            }
            assertEquals(ErrorCode.POINT_EXCEEDS_MAXIMUM, ex.errorCode)
        }

        @Test
        fun `pointAmount가 정확히 주문금액의 50%일 때 정상 처리`() {
            // given
            val order = mockk<Order>()
            every { order.totalAmount } returns 10000L  // maxUsable = 5000
            every { order.id } returns 1L
            val userPoint = createUserPoint(balance = 10000L)
            every { userPointRepository.findByUserId(1L) } returns userPoint
            every { pointHistoryRepository.save(any()) } returns createPointHistory(amount = 5000L, balance = 5000L)

            // when — 5000 == 5000 (경계값, 초과 아님)
            val result = pointService.validateAndUsePoints(userId = 1L, pointAmount = 5000L, order = order)

            // then
            assertEquals(5000L, result)
            assertEquals(5000L, userPoint.balance)
        }

        @Test
        fun `잔액 부족 시 POINT_INSUFFICIENT 예외 발생`() {
            // given
            val order = mockk<Order>()
            every { order.totalAmount } returns 20000L  // maxUsable = 10000
            val userPoint = createUserPoint(balance = 500L)
            every { userPointRepository.findByUserId(1L) } returns userPoint

            // when / then
            val ex = assertThrows<CustomException> {
                pointService.validateAndUsePoints(userId = 1L, pointAmount = 1000L, order = order)
            }
            assertEquals(ErrorCode.POINT_INSUFFICIENT, ex.errorCode)
        }

        @Test
        fun `정상 차감 시 balance 감소, 이력 저장, pointAmount 반환`() {
            // given
            val order = mockk<Order>()
            every { order.totalAmount } returns 20000L  // maxUsable = 10000
            every { order.id } returns 10L
            val userPoint = createUserPoint(balance = 5000L)
            every { userPointRepository.findByUserId(1L) } returns userPoint
            every { pointHistoryRepository.save(any()) } returns createPointHistory(
                type = PointHistoryType.USE_PAYMENT, amount = 2000L, balance = 3000L
            )

            // when
            val result = pointService.validateAndUsePoints(userId = 1L, pointAmount = 2000L, order = order)

            // then
            assertEquals(2000L, result)
            assertEquals(3000L, userPoint.balance)
            verify(exactly = 1) { pointHistoryRepository.save(any()) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class RefundPoints {

        @Test
        fun `USE_PAYMENT 이력 없으면 아무것도 하지 않음`() {
            // given
            every {
                pointHistoryRepository.findByUserIdAndRelatedIdAndType(1L, 10L, PointHistoryType.USE_PAYMENT)
            } returns null

            // when
            pointService.refundPoints(userId = 1L, orderId = 10L)

            // then
            verify(exactly = 0) { userPointRepository.findByUserId(any()) }
            verify(exactly = 0) { pointHistoryRepository.save(any()) }
        }

        @Test
        fun `USE_PAYMENT 이력 있으면 balance 복원 및 REFUND_PAYMENT 이력 저장`() {
            // given
            val useHistory = createPointHistory(
                type = PointHistoryType.USE_PAYMENT, amount = 1000L, balance = 4000L, relatedId = 10L
            )
            val userPoint = createUserPoint(balance = 4000L)
            every {
                pointHistoryRepository.findByUserIdAndRelatedIdAndType(1L, 10L, PointHistoryType.USE_PAYMENT)
            } returns useHistory
            every { userPointRepository.findByUserId(1L) } returns userPoint
            every { pointHistoryRepository.save(any()) } returns createPointHistory(
                type = PointHistoryType.REFUND_PAYMENT, amount = 1000L, balance = 5000L, relatedId = 10L
            )

            // when
            pointService.refundPoints(userId = 1L, orderId = 10L)

            // then
            assertEquals(5000L, userPoint.balance)
            verify(exactly = 1) { pointHistoryRepository.save(any()) }
        }

        @Test
        fun `환불 시 UserPoint 없으면 자동 생성 후 balance 복원`() {
            // given
            val useHistory = createPointHistory(
                type = PointHistoryType.USE_PAYMENT, amount = 1000L, balance = 0L, relatedId = 10L
            )
            val newPoint = createUserPoint(balance = 0L)
            every {
                pointHistoryRepository.findByUserIdAndRelatedIdAndType(1L, 10L, PointHistoryType.USE_PAYMENT)
            } returns useHistory
            every { userPointRepository.findByUserId(1L) } returns null
            every { userPointRepository.save(any()) } returns newPoint
            every { pointHistoryRepository.save(any()) } returns createPointHistory(
                type = PointHistoryType.REFUND_PAYMENT, amount = 1000L, balance = 1000L, relatedId = 10L
            )

            // when
            pointService.refundPoints(userId = 1L, orderId = 10L)

            // then
            assertEquals(1000L, newPoint.balance)
            verify(exactly = 1) { userPointRepository.save(any()) }
            verify(exactly = 1) { pointHistoryRepository.save(any()) }
        }
    }
}
