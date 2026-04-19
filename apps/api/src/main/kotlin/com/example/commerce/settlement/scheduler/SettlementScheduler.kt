package com.example.commerce.settlement.scheduler

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.settlement.service.SettlementService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SettlementScheduler(
    private val settlementService: SettlementService,
) {
    private val logger = LoggerFactory.getLogger(SettlementScheduler::class.java)

    @Scheduled(cron = "0 5 0 * * *")
    fun createDailySettlement() {
        val yesterday = LocalDate.now().minusDays(1)
        try {
            settlementService.createSettlement(yesterday)
            logger.info("일별 정산 생성 완료: $yesterday")
        } catch (e: CustomException) {
            if (e.errorCode == ErrorCode.SETTLEMENT_ALREADY_EXISTS) {
                logger.info("정산 이미 존재: $yesterday")
            } else {
                logger.error("정산 생성 실패: $yesterday", e)
            }
        }
    }
}
