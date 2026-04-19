package com.example.commerce.settlement.repository

import com.example.commerce.settlement.entity.Settlement
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface SettlementRepository : JpaRepository<Settlement, Long> {
    fun findBySettlementDate(date: LocalDate): Settlement?
    fun findAllBySettlementDateBetweenOrderBySettlementDateDesc(
        from: LocalDate,
        to: LocalDate,
        pageable: Pageable,
    ): Page<Settlement>
    fun findAllBySettlementDateBetween(from: LocalDate, to: LocalDate): List<Settlement>
}
