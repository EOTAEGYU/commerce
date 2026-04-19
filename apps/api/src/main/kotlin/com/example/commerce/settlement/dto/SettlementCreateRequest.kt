package com.example.commerce.settlement.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class SettlementCreateRequest(
    @field:NotNull val date: LocalDate,
)
