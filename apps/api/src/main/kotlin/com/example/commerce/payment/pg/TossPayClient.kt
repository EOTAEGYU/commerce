package com.example.commerce.payment.pg

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.payment.config.PgProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.Base64

@Component
class TossPayClient(private val pgProperties: PgProperties) {

    private val restClient = RestClient.create()
    private val confirmUrl = "https://api.tosspayments.com/v1/payments/confirm"

    data class ConfirmRequest(
        val paymentKey: String,
        val orderId: String,
        val amount: Long,
    )

    data class ConfirmResponse(
        val paymentKey: String,
        val orderId: String,
        val status: String,
        val method: String?,
    )

    fun confirm(request: ConfirmRequest): ConfirmResponse {
        val encodedKey = Base64.getEncoder()
            .encodeToString("${pgProperties.toss.secretKey}:".toByteArray())
        try {
            return restClient.post()
                .uri(confirmUrl)
                .header("Authorization", "Basic $encodedKey")
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(ConfirmResponse::class.java)
                ?: throw CustomException(ErrorCode.PG_PAYMENT_FAILED)
        } catch (e: RestClientException) {
            throw CustomException(ErrorCode.PG_PAYMENT_FAILED)
        }
    }
}
