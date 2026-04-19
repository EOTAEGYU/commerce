package com.example.commerce.settlement.dto

import com.example.commerce.settlement.entity.SettlementStatus
import jakarta.validation.constraints.NotNull

data class SettlementStatusUpdateRequest(
    @field:NotNull val status: SettlementStatus,
)
