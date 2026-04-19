package com.example.commerce.settlement.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.payment.entity.Payment
import com.example.commerce.payment.entity.PaymentMethod
import com.example.commerce.payment.entity.PaymentStatus
import com.example.commerce.payment.repository.PaymentRepository
import com.example.commerce.settlement.entity.Settlement
import com.example.commerce.settlement.entity.SettlementStatus
import com.example.commerce.settlement.repository.SettlementRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockKExtension::class)
class SettlementServiceTest {

    @MockK lateinit var settlementRepository: SettlementRepository
    @MockK lateinit var paymentRepository: PaymentRepository

    @InjectMockKs
    lateinit var settlementService: SettlementService

    private val today = LocalDate.of(2026, 4, 19)
    private val yesterday = today.minusDays(1)

    private fun createSettlement(
        id: Long = 1L,
        settlementDate: LocalDate = yesterday,
        status: SettlementStatus = SettlementStatus.PENDING,
        orderCount: Int = 3,
        totalOrderAmount: Long = 120000L,
        totalDiscountAmount: Long = 10000L,
        totalPointAmount: Long = 5000L,
        totalNetAmount: Long = 105000L,
        totalEarnedPoints: Long = 1050L,
    ) = Settlement(
        id = id,
        settlementDate = settlementDate,
        status = status,
        orderCount = orderCount,
        totalOrderAmount = totalOrderAmount,
        totalDiscountAmount = totalDiscountAmount,
        totalPointAmount = totalPointAmount,
        totalNetAmount = totalNetAmount,
        totalEarnedPoints = totalEarnedPoints,
    )

    private fun createPayment(
        id: Long = 1L,
        orderId: Long = 100L,
        userId: Long = 1L,
        amount: Long = 35000L,
        discountAmount: Long = 3000L,
        pointAmount: Long = 2000L,
        earnedPoints: Long = 350L,
    ) = Payment(
        id = id,
        orderId = orderId,
        userId = userId,
        amount = amount,
        discountAmount = discountAmount,
        pointAmount = pointAmount,
        earnedPoints = earnedPoints,
        method = PaymentMethod.CARD,
        status = PaymentStatus.COMPLETED,
    )

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class CreateSettlement {

        @Test
        fun `정상 날짜로 정산 생성 시 저장 후 응답 반환`() {
            // given
            val date = yesterday
            val payments = listOf(
                createPayment(id = 1L, amount = 35000L, discountAmount = 3000L, pointAmount = 2000L, earnedPoints = 350L),
                createPayment(id = 2L, orderId = 101L, amount = 50000L, discountAmount = 5000L, pointAmount = 2000L, earnedPoints = 500L),
            )
            val savedSettlement = createSettlement(
                orderCount = 2,
                totalOrderAmount = 97000L,
                totalDiscountAmount = 8000L,
                totalPointAmount = 4000L,
                totalNetAmount = 85000L,
                totalEarnedPoints = 850L,
            )
            every { settlementRepository.findBySettlementDate(date) } returns null
            every {
                paymentRepository.findAllByStatusAndCreatedAtBetween(
                    PaymentStatus.COMPLETED,
                    date.atStartOfDay(),
                    date.atTime(23, 59, 59),
                )
            } returns payments
            every { settlementRepository.save(any()) } returns savedSettlement

            // when
            val result = settlementService.createSettlement(date)

            // then
            assertEquals(2, result.orderCount)
            assertEquals(85000L, result.totalNetAmount)
            verify(exactly = 1) { settlementRepository.save(any()) }
        }

        @Test
        fun `미래 날짜 정산 시도 시 SETTLEMENT_DATE_INVALID 예외 발생`() {
            // given
            val futureDate = today.plusDays(1)

            // when / then
            val ex = assertThrows<CustomException> {
                settlementService.createSettlement(futureDate)
            }
            assertEquals(ErrorCode.SETTLEMENT_DATE_INVALID, ex.errorCode)
            verify(exactly = 0) { settlementRepository.save(any()) }
        }

        @Test
        fun `오늘 날짜는 미래가 아니므로 예외 없이 처리 시도`() {
            // given — today is not after today, so no DATE_INVALID
            every { settlementRepository.findBySettlementDate(today) } returns null
            every {
                paymentRepository.findAllByStatusAndCreatedAtBetween(any(), any(), any())
            } returns emptyList()
            every { settlementRepository.save(any()) } returns createSettlement(settlementDate = today, orderCount = 0)

            // when / then
            assertDoesNotThrow { settlementService.createSettlement(today) }
        }

        @Test
        fun `동일 날짜 중복 정산 시도 시 SETTLEMENT_ALREADY_EXISTS 예외 발생`() {
            // given
            every { settlementRepository.findBySettlementDate(yesterday) } returns createSettlement()

            // when / then
            val ex = assertThrows<CustomException> {
                settlementService.createSettlement(yesterday)
            }
            assertEquals(ErrorCode.SETTLEMENT_ALREADY_EXISTS, ex.errorCode)
            verify(exactly = 0) { settlementRepository.save(any()) }
        }

        @Test
        fun `결제 내역 없는 날짜 정산 시 orderCount 0으로 생성`() {
            // given
            val date = yesterday
            val emptySettlement = createSettlement(
                orderCount = 0,
                totalOrderAmount = 0L,
                totalDiscountAmount = 0L,
                totalPointAmount = 0L,
                totalNetAmount = 0L,
                totalEarnedPoints = 0L,
            )
            every { settlementRepository.findBySettlementDate(date) } returns null
            every {
                paymentRepository.findAllByStatusAndCreatedAtBetween(any(), any(), any())
            } returns emptyList()
            every { settlementRepository.save(any()) } returns emptySettlement

            // when
            val result = settlementService.createSettlement(date)

            // then
            assertEquals(0, result.orderCount)
            assertEquals(0L, result.totalNetAmount)
            verify(exactly = 1) { settlementRepository.save(any()) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class GetSettlement {

        @Test
        fun `존재하는 id로 조회 시 정산 응답 반환`() {
            // given
            val settlement = createSettlement(id = 1L)
            every { settlementRepository.findById(1L) } returns Optional.of(settlement)

            // when
            val result = settlementService.getSettlement(1L)

            // then
            assertEquals(1L, result.id)
            assertEquals(yesterday, result.settlementDate)
            assertEquals(SettlementStatus.PENDING, result.status)
        }

        @Test
        fun `존재하지 않는 id로 조회 시 SETTLEMENT_NOT_FOUND 예외 발생`() {
            // given
            every { settlementRepository.findById(999L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> {
                settlementService.getSettlement(999L)
            }
            assertEquals(ErrorCode.SETTLEMENT_NOT_FOUND, ex.errorCode)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class GetSettlements {

        @Test
        fun `from-to 지정 시 해당 기간 정산 목록 페이징 반환`() {
            // given
            val from = today.minusDays(7)
            val to = today
            val pageable = PageRequest.of(0, 20)
            val settlements = listOf(createSettlement(id = 1L), createSettlement(id = 2L, settlementDate = today.minusDays(2)))
            val page = PageImpl(settlements, pageable, 2L)
            every {
                settlementRepository.findAllBySettlementDateBetweenOrderBySettlementDateDesc(from, to, pageable)
            } returns page

            // when
            val result = settlementService.getSettlements(from, to, pageable)

            // then
            assertEquals(2, result.totalElements)
            assertEquals(1L, result.content[0].id)
        }

        @Test
        fun `from-to null 전달 시 기본값(3개월) 으로 조회`() {
            // given
            val pageable = PageRequest.of(0, 20)
            val emptyPage = PageImpl<Settlement>(emptyList(), pageable, 0L)
            every {
                settlementRepository.findAllBySettlementDateBetweenOrderBySettlementDateDesc(any(), any(), any())
            } returns emptyPage

            // when
            val result = settlementService.getSettlements(null, null, pageable)

            // then
            assertEquals(0, result.totalElements)
            assertEquals(true, result.isEmpty)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class UpdateStatus {

        @Test
        fun `PENDING 상태에서 CONFIRMED 전이 성공`() {
            // given
            val settlement = createSettlement(id = 1L, status = SettlementStatus.PENDING)
            every { settlementRepository.findById(1L) } returns Optional.of(settlement)

            // when
            val result = settlementService.updateStatus(1L, SettlementStatus.CONFIRMED)

            // then
            assertEquals(SettlementStatus.CONFIRMED, result.status)
            // dirty-checking — no explicit save call
            verify(exactly = 0) { settlementRepository.save(any()) }
        }

        @Test
        fun `CONFIRMED 상태에서 PAID 전이 성공`() {
            // given
            val settlement = createSettlement(id = 1L, status = SettlementStatus.CONFIRMED)
            every { settlementRepository.findById(1L) } returns Optional.of(settlement)

            // when
            val result = settlementService.updateStatus(1L, SettlementStatus.PAID)

            // then
            assertEquals(SettlementStatus.PAID, result.status)
            verify(exactly = 0) { settlementRepository.save(any()) }
        }

        @Test
        fun `PENDING에서 PAID로 역방향 전이 시 SETTLEMENT_INVALID_STATUS 예외 발생`() {
            // given
            val settlement = createSettlement(id = 1L, status = SettlementStatus.PENDING)
            every { settlementRepository.findById(1L) } returns Optional.of(settlement)

            // when / then
            val ex = assertThrows<CustomException> {
                settlementService.updateStatus(1L, SettlementStatus.PAID)
            }
            assertEquals(ErrorCode.SETTLEMENT_INVALID_STATUS, ex.errorCode)
        }

        @Test
        fun `CONFIRMED에서 PENDING으로 역방향 전이 시 SETTLEMENT_INVALID_STATUS 예외 발생`() {
            // given
            val settlement = createSettlement(id = 1L, status = SettlementStatus.CONFIRMED)
            every { settlementRepository.findById(1L) } returns Optional.of(settlement)

            // when / then
            val ex = assertThrows<CustomException> {
                settlementService.updateStatus(1L, SettlementStatus.PENDING)
            }
            assertEquals(ErrorCode.SETTLEMENT_INVALID_STATUS, ex.errorCode)
        }

        @Test
        fun `PAID에서 CONFIRMED로 역방향 전이 시 SETTLEMENT_INVALID_STATUS 예외 발생`() {
            // given
            val settlement = createSettlement(id = 1L, status = SettlementStatus.PAID)
            every { settlementRepository.findById(1L) } returns Optional.of(settlement)

            // when / then
            val ex = assertThrows<CustomException> {
                settlementService.updateStatus(1L, SettlementStatus.CONFIRMED)
            }
            assertEquals(ErrorCode.SETTLEMENT_INVALID_STATUS, ex.errorCode)
        }

        @Test
        fun `존재하지 않는 정산 상태 변경 시도 시 SETTLEMENT_NOT_FOUND 예외 발생`() {
            // given
            every { settlementRepository.findById(999L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> {
                settlementService.updateStatus(999L, SettlementStatus.CONFIRMED)
            }
            assertEquals(ErrorCode.SETTLEMENT_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `CONFIRMED 전이 시 confirmedAt 기록됨`() {
            // given
            val settlement = createSettlement(id = 1L, status = SettlementStatus.PENDING)
            every { settlementRepository.findById(1L) } returns Optional.of(settlement)

            // when
            settlementService.updateStatus(1L, SettlementStatus.CONFIRMED)

            // then — confirmedAt must be set (non-null)
            val confirmedAt = settlement.confirmedAt
            assertEquals(false, confirmedAt == null)
        }

        @Test
        fun `PAID 전이 시 paidAt 기록됨`() {
            // given
            val settlement = createSettlement(id = 1L, status = SettlementStatus.CONFIRMED)
            every { settlementRepository.findById(1L) } returns Optional.of(settlement)

            // when
            settlementService.updateStatus(1L, SettlementStatus.PAID)

            // then — paidAt must be set (non-null)
            val paidAt = settlement.paidAt
            assertEquals(false, paidAt == null)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class GetStats {

        @Test
        fun `정상 기간 조회 시 집계값 올바르게 반환`() {
            // given
            val from = today.minusDays(2)
            val to = today
            val settlements = listOf(
                createSettlement(id = 1L, totalNetAmount = 100000L, orderCount = 5),
                createSettlement(id = 2L, totalNetAmount = 200000L, orderCount = 8),
            )
            every { settlementRepository.findAllBySettlementDateBetween(from, to) } returns settlements

            // when
            val result = settlementService.getStats(from, to)

            // then
            assertEquals(3, result.totalDays)       // minusDays(2) to today = 3 days
            assertEquals(2, result.settledDays)
            assertEquals(13, result.totalOrderCount)
            assertEquals(300000L, result.totalNetAmount)
            assertEquals(150000L, result.avgDailyNetAmount) // 300000 / 2 settled days
        }

        @Test
        fun `정산 데이터 없는 기간 조회 시 모든 금액 0 반환`() {
            // given
            val from = today.minusDays(1)
            val to = today
            every { settlementRepository.findAllBySettlementDateBetween(from, to) } returns emptyList()

            // when
            val result = settlementService.getStats(from, to)

            // then
            assertEquals(2, result.totalDays)
            assertEquals(0, result.settledDays)
            assertEquals(0L, result.totalNetAmount)
            assertEquals(0L, result.avgDailyNetAmount)
        }

        @Test
        fun `단일 정산 조회 시 avgDailyNetAmount는 totalNetAmount와 동일`() {
            // given
            val from = yesterday
            val to = yesterday
            val settlements = listOf(createSettlement(id = 1L, totalNetAmount = 50000L, orderCount = 2))
            every { settlementRepository.findAllBySettlementDateBetween(from, to) } returns settlements

            // when
            val result = settlementService.getStats(from, to)

            // then
            assertEquals(1, result.totalDays)
            assertEquals(1, result.settledDays)
            assertEquals(50000L, result.totalNetAmount)
            assertEquals(50000L, result.avgDailyNetAmount)
        }

        @Test
        fun `totalDiscountAmount와 totalPointAmount 집계값 정확히 합산`() {
            // given
            val from = today.minusDays(2)
            val to = today
            val settlements = listOf(
                createSettlement(id = 1L, totalDiscountAmount = 3000L, totalPointAmount = 1000L),
                createSettlement(id = 2L, totalDiscountAmount = 7000L, totalPointAmount = 4000L),
            )
            every { settlementRepository.findAllBySettlementDateBetween(from, to) } returns settlements

            // when
            val result = settlementService.getStats(from, to)

            // then
            assertEquals(10000L, result.totalDiscountAmount)
            assertEquals(5000L, result.totalPointAmount)
        }
    }
}
