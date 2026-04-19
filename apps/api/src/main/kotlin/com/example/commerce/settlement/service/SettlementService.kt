package com.example.commerce.settlement.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.payment.entity.PaymentStatus
import com.example.commerce.payment.repository.PaymentRepository
import com.example.commerce.settlement.dto.SettlementCreateRequest
import com.example.commerce.settlement.dto.SettlementResponse
import com.example.commerce.settlement.dto.SettlementStatsResponse
import com.example.commerce.settlement.entity.Settlement
import com.example.commerce.settlement.entity.SettlementStatus
import com.example.commerce.settlement.repository.SettlementRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val paymentRepository: PaymentRepository,
) {
    @Transactional
    fun createSettlement(date: LocalDate): SettlementResponse {
        if (date.isAfter(LocalDate.now())) throw CustomException(ErrorCode.SETTLEMENT_DATE_INVALID)
        if (settlementRepository.findBySettlementDate(date) != null) throw CustomException(ErrorCode.SETTLEMENT_ALREADY_EXISTS)

        val from = date.atStartOfDay()
        val to = date.atTime(23, 59, 59)
        val payments = paymentRepository.findAllByStatusAndCreatedAtBetween(PaymentStatus.COMPLETED, from, to)

        val settlement = Settlement.create(date, payments)
        return SettlementResponse.from(settlementRepository.save(settlement))
    }

    fun getSettlements(from: LocalDate?, to: LocalDate?, pageable: Pageable): Page<SettlementResponse> {
        val effectiveFrom = from ?: LocalDate.now().minusMonths(3)
        val effectiveTo = to ?: LocalDate.now()
        return settlementRepository.findAllBySettlementDateBetweenOrderBySettlementDateDesc(effectiveFrom, effectiveTo, pageable)
            .map { SettlementResponse.from(it) }
    }

    fun getSettlement(id: Long): SettlementResponse {
        val settlement = settlementRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.SETTLEMENT_NOT_FOUND) }
        return SettlementResponse.from(settlement)
    }

    @Transactional
    fun updateStatus(id: Long, newStatus: SettlementStatus): SettlementResponse {
        val settlement = settlementRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.SETTLEMENT_NOT_FOUND) }

        val valid = (settlement.status == SettlementStatus.PENDING && newStatus == SettlementStatus.CONFIRMED) ||
            (settlement.status == SettlementStatus.CONFIRMED && newStatus == SettlementStatus.PAID)
        if (!valid) throw CustomException(ErrorCode.SETTLEMENT_INVALID_STATUS)

        settlement.status = newStatus
        when (newStatus) {
            SettlementStatus.CONFIRMED -> settlement.confirmedAt = LocalDateTime.now()
            SettlementStatus.PAID -> settlement.paidAt = LocalDateTime.now()
            else -> {}
        }
        return SettlementResponse.from(settlement)
    }

    fun getStats(from: LocalDate, to: LocalDate): SettlementStatsResponse {
        val settlements = settlementRepository.findAllBySettlementDateBetween(from, to)
        val totalDays = (ChronoUnit.DAYS.between(from, to) + 1).toInt()
        return SettlementStatsResponse(
            from = from,
            to = to,
            totalDays = totalDays,
            settledDays = settlements.size,
            totalOrderCount = settlements.sumOf { it.orderCount },
            totalOrderAmount = settlements.sumOf { it.totalOrderAmount },
            totalDiscountAmount = settlements.sumOf { it.totalDiscountAmount },
            totalPointAmount = settlements.sumOf { it.totalPointAmount },
            totalNetAmount = settlements.sumOf { it.totalNetAmount },
            totalEarnedPoints = settlements.sumOf { it.totalEarnedPoints },
            avgDailyNetAmount = if (settlements.isEmpty()) 0L else settlements.sumOf { it.totalNetAmount } / settlements.size,
        )
    }
}
