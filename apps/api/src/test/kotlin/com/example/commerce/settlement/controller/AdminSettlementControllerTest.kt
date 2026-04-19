package com.example.commerce.settlement.controller

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.common.security.JwtProvider
import com.example.commerce.common.security.SecurityConfig
import com.example.commerce.settlement.dto.SettlementCreateRequest
import com.example.commerce.settlement.dto.SettlementResponse
import com.example.commerce.settlement.dto.SettlementStatsResponse
import com.example.commerce.settlement.dto.SettlementStatusUpdateRequest
import com.example.commerce.settlement.entity.SettlementStatus
import com.example.commerce.settlement.service.SettlementService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(AdminSettlementController::class)
@Import(SecurityConfig::class)
class AdminSettlementControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var settlementService: SettlementService
    @MockitoBean lateinit var jwtProvider: JwtProvider

    private val now = LocalDateTime.of(2026, 4, 19, 12, 0, 0)
    private val yesterday = LocalDate.of(2026, 4, 18)

    private fun createSettlementResponse(
        id: Long = 1L,
        settlementDate: LocalDate = yesterday,
        status: SettlementStatus = SettlementStatus.PENDING,
        orderCount: Int = 3,
        totalOrderAmount: Long = 120000L,
        totalDiscountAmount: Long = 10000L,
        totalPointAmount: Long = 5000L,
        totalNetAmount: Long = 105000L,
        totalEarnedPoints: Long = 1050L,
        confirmedAt: LocalDateTime? = null,
        paidAt: LocalDateTime? = null,
    ) = SettlementResponse(
        id = id,
        settlementDate = settlementDate,
        status = status,
        orderCount = orderCount,
        totalOrderAmount = totalOrderAmount,
        totalDiscountAmount = totalDiscountAmount,
        totalPointAmount = totalPointAmount,
        totalNetAmount = totalNetAmount,
        totalEarnedPoints = totalEarnedPoints,
        confirmedAt = confirmedAt,
        paidAt = paidAt,
        createdAt = now,
    )

    private fun adminAuth() = authentication(
        UsernamePasswordAuthenticationToken(1L, null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
    )

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class CreateSettlement {

        @Test
        fun `정상 날짜로 정산 생성 시 201 반환`() {
            val request = SettlementCreateRequest(date = yesterday)
            val response = createSettlementResponse()
            given(settlementService.createSettlement(eq(yesterday))).willReturn(response)

            mockMvc.post("/api/admin/settlements") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isCreated() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.status") { value("PENDING") }
                jsonPath("$.data.orderCount") { value(3) }
                jsonPath("$.data.totalNetAmount") { value(105000) }
            }
        }

        @Test
        fun `미래 날짜로 정산 생성 시도 시 400 반환`() {
            val futureDate = LocalDate.of(2026, 4, 20)
            val request = SettlementCreateRequest(date = futureDate)
            given(settlementService.createSettlement(eq(futureDate)))
                .willThrow(CustomException(ErrorCode.SETTLEMENT_DATE_INVALID))

            mockMvc.post("/api/admin/settlements") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("SETTLEMENT_DATE_INVALID") }
            }
        }

        @Test
        fun `중복 날짜로 정산 생성 시도 시 409 반환`() {
            val request = SettlementCreateRequest(date = yesterday)
            given(settlementService.createSettlement(eq(yesterday)))
                .willThrow(CustomException(ErrorCode.SETTLEMENT_ALREADY_EXISTS))

            mockMvc.post("/api/admin/settlements") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("SETTLEMENT_ALREADY_EXISTS") }
            }
        }

        @Test
        fun `인증 없이 정산 생성 시도 시 401 반환`() {
            val request = SettlementCreateRequest(date = yesterday)

            mockMvc.post("/api/admin/settlements") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class GetSettlements {

        @Test
        fun `기간 파라미터 없이 정산 목록 조회 시 200 반환`() {
            val page = PageImpl(listOf(createSettlementResponse(id = 1L), createSettlementResponse(id = 2L)))
            given(settlementService.getSettlements(anyOrNull(), anyOrNull(), any())).willReturn(page)

            mockMvc.get("/api/admin/settlements") {
                with(adminAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.content[0].id") { value(1) }
                jsonPath("$.data.content[1].id") { value(2) }
                jsonPath("$.data.totalElements") { value(2) }
            }
        }

        @Test
        fun `from-to 기간 파라미터 지정 시 200 반환`() {
            val page = PageImpl(listOf(createSettlementResponse()))
            given(settlementService.getSettlements(anyOrNull(), anyOrNull(), any())).willReturn(page)

            mockMvc.get("/api/admin/settlements?from=2026-04-15&to=2026-04-18") {
                with(adminAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.content[0].settlementDate") { value("2026-04-18") }
            }
        }

        @Test
        fun `정산 내역 없는 기간 조회 시 빈 목록과 200 반환`() {
            val emptyPage = PageImpl<SettlementResponse>(emptyList())
            given(settlementService.getSettlements(anyOrNull(), anyOrNull(), any())).willReturn(emptyPage)

            mockMvc.get("/api/admin/settlements") {
                with(adminAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.content") { isEmpty() }
            }
        }

        @Test
        fun `인증 없이 목록 조회 시 401 반환`() {
            mockMvc.get("/api/admin/settlements").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class GetSettlement {

        @Test
        fun `존재하는 정산 단건 조회 시 200 반환`() {
            val response = createSettlementResponse(id = 1L)
            given(settlementService.getSettlement(1L)).willReturn(response)

            mockMvc.get("/api/admin/settlements/1") {
                with(adminAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.totalNetAmount") { value(105000) }
            }
        }

        @Test
        fun `존재하지 않는 정산 단건 조회 시 404 반환`() {
            given(settlementService.getSettlement(999L))
                .willThrow(CustomException(ErrorCode.SETTLEMENT_NOT_FOUND))

            mockMvc.get("/api/admin/settlements/999") {
                with(adminAuth())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("SETTLEMENT_NOT_FOUND") }
            }
        }

        @Test
        fun `인증 없이 단건 조회 시 401 반환`() {
            mockMvc.get("/api/admin/settlements/1").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class UpdateStatus {

        @Test
        fun `PENDING에서 CONFIRMED 상태 변경 시 200 반환`() {
            val request = SettlementStatusUpdateRequest(status = SettlementStatus.CONFIRMED)
            val response = createSettlementResponse(id = 1L, status = SettlementStatus.CONFIRMED, confirmedAt = now)
            given(settlementService.updateStatus(eq(1L), eq(SettlementStatus.CONFIRMED))).willReturn(response)

            mockMvc.patch("/api/admin/settlements/1/status") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.status") { value("CONFIRMED") }
            }
        }

        @Test
        fun `CONFIRMED에서 PAID 상태 변경 시 200 반환`() {
            val request = SettlementStatusUpdateRequest(status = SettlementStatus.PAID)
            val response = createSettlementResponse(
                id = 1L,
                status = SettlementStatus.PAID,
                confirmedAt = now.minusHours(1),
                paidAt = now,
            )
            given(settlementService.updateStatus(eq(1L), eq(SettlementStatus.PAID))).willReturn(response)

            mockMvc.patch("/api/admin/settlements/1/status") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.status") { value("PAID") }
            }
        }

        @Test
        fun `허용되지 않는 상태 전이 시 400 반환`() {
            val request = SettlementStatusUpdateRequest(status = SettlementStatus.PAID)
            given(settlementService.updateStatus(eq(1L), eq(SettlementStatus.PAID)))
                .willThrow(CustomException(ErrorCode.SETTLEMENT_INVALID_STATUS))

            mockMvc.patch("/api/admin/settlements/1/status") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("SETTLEMENT_INVALID_STATUS") }
            }
        }

        @Test
        fun `존재하지 않는 정산 상태 변경 시도 시 404 반환`() {
            val request = SettlementStatusUpdateRequest(status = SettlementStatus.CONFIRMED)
            given(settlementService.updateStatus(eq(999L), eq(SettlementStatus.CONFIRMED)))
                .willThrow(CustomException(ErrorCode.SETTLEMENT_NOT_FOUND))

            mockMvc.patch("/api/admin/settlements/999/status") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(adminAuth())
                with(csrf())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("SETTLEMENT_NOT_FOUND") }
            }
        }

        @Test
        fun `인증 없이 상태 변경 시도 시 401 반환`() {
            val request = SettlementStatusUpdateRequest(status = SettlementStatus.CONFIRMED)

            mockMvc.patch("/api/admin/settlements/1/status") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class GetStats {

        @Test
        fun `기간 지정 집계 조회 시 200 반환`() {
            val from = LocalDate.of(2026, 4, 1)
            val to = LocalDate.of(2026, 4, 18)
            val statsResponse = SettlementStatsResponse(
                from = from,
                to = to,
                totalDays = 18,
                settledDays = 15,
                totalOrderCount = 120,
                totalOrderAmount = 6000000L,
                totalDiscountAmount = 300000L,
                totalPointAmount = 150000L,
                totalNetAmount = 5550000L,
                totalEarnedPoints = 55500L,
                avgDailyNetAmount = 370000L,
            )
            given(settlementService.getStats(any(), any())).willReturn(statsResponse)

            mockMvc.get("/api/admin/settlements/stats?from=2026-04-01&to=2026-04-18") {
                with(adminAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.totalDays") { value(18) }
                jsonPath("$.data.settledDays") { value(15) }
                jsonPath("$.data.totalNetAmount") { value(5550000) }
                jsonPath("$.data.avgDailyNetAmount") { value(370000) }
            }
        }

        @Test
        fun `정산 데이터 없는 기간 집계 시 모든 금액 0으로 200 반환`() {
            val from = LocalDate.of(2026, 4, 1)
            val to = LocalDate.of(2026, 4, 5)
            val emptyStats = SettlementStatsResponse(
                from = from,
                to = to,
                totalDays = 5,
                settledDays = 0,
                totalOrderCount = 0,
                totalOrderAmount = 0L,
                totalDiscountAmount = 0L,
                totalPointAmount = 0L,
                totalNetAmount = 0L,
                totalEarnedPoints = 0L,
                avgDailyNetAmount = 0L,
            )
            given(settlementService.getStats(any(), any())).willReturn(emptyStats)

            mockMvc.get("/api/admin/settlements/stats?from=2026-04-01&to=2026-04-05") {
                with(adminAuth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.settledDays") { value(0) }
                jsonPath("$.data.totalNetAmount") { value(0) }
                jsonPath("$.data.avgDailyNetAmount") { value(0) }
            }
        }

        @Test
        fun `인증 없이 집계 조회 시도 시 401 반환`() {
            mockMvc.get("/api/admin/settlements/stats?from=2026-04-01&to=2026-04-18").andExpect {
                status { isUnauthorized() }
            }
        }
    }
}
