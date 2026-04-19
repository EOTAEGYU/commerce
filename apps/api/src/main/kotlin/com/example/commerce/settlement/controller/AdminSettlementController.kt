package com.example.commerce.settlement.controller

import com.example.commerce.common.ApiResponse
import com.example.commerce.settlement.dto.SettlementCreateRequest
import com.example.commerce.settlement.dto.SettlementResponse
import com.example.commerce.settlement.dto.SettlementStatsResponse
import com.example.commerce.settlement.dto.SettlementStatusUpdateRequest
import com.example.commerce.settlement.service.SettlementService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/settlements")
class AdminSettlementController(
    private val settlementService: SettlementService,
) {
    @PostMapping
    fun createSettlement(
        @RequestBody @Valid request: SettlementCreateRequest,
    ): ResponseEntity<ApiResponse<SettlementResponse>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(settlementService.createSettlement(request.date)))

    @GetMapping
    fun getSettlements(
        @RequestParam from: LocalDate?,
        @RequestParam to: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<Page<SettlementResponse>>> =
        ResponseEntity.ok(
            ApiResponse.success(settlementService.getSettlements(from, to, PageRequest.of(page, size)))
        )

    @GetMapping("/stats")
    fun getStats(
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ): ResponseEntity<ApiResponse<SettlementStatsResponse>> =
        ResponseEntity.ok(ApiResponse.success(settlementService.getStats(from, to)))

    @GetMapping("/{id}")
    fun getSettlement(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<SettlementResponse>> =
        ResponseEntity.ok(ApiResponse.success(settlementService.getSettlement(id)))

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody @Valid request: SettlementStatusUpdateRequest,
    ): ResponseEntity<ApiResponse<SettlementResponse>> =
        ResponseEntity.ok(ApiResponse.success(settlementService.updateStatus(id, request.status)))
}
