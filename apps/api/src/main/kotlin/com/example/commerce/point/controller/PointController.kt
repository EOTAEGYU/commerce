package com.example.commerce.point.controller

import com.example.commerce.common.ApiResponse
import com.example.commerce.point.dto.PointBalanceResponse
import com.example.commerce.point.dto.PointHistoryResponse
import com.example.commerce.point.service.PointService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/points")
class PointController(
    private val pointService: PointService,
) {
    @GetMapping("/me")
    fun getBalance(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<ApiResponse<PointBalanceResponse>> =
        ResponseEntity.ok(ApiResponse.success(pointService.getBalance(userId)))

    @GetMapping("/history")
    fun getHistory(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<Page<PointHistoryResponse>>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return ResponseEntity.ok(ApiResponse.success(pointService.getHistory(userId, pageable)))
    }
}
