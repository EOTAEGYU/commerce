package com.example.commerce.payment.pg

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.payment.config.PgProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class KakaoPayClient(private val pgProperties: PgProperties) {

    private val restClient = RestClient.create()
    private val baseUrl = "https://open-api.kakaopay.com/online/v1/payment"

    data class ReadyRequest(
        val cid: String,
        @JsonProperty("partner_order_id") val partnerOrderId: String,
        @JsonProperty("partner_user_id") val partnerUserId: String,
        @JsonProperty("item_name") val itemName: String,
        val quantity: Int,
        @JsonProperty("total_amount") val totalAmount: Long,
        @JsonProperty("tax_free_amount") val taxFreeAmount: Long,
        @JsonProperty("approval_url") val approvalUrl: String,
        @JsonProperty("cancel_url") val cancelUrl: String,
        @JsonProperty("fail_url") val failUrl: String,
    )

    data class ReadyResponse(
        val tid: String,
        @JsonProperty("next_redirect_pc_url") val nextRedirectPcUrl: String,
    )

    data class ApproveRequest(
        val cid: String,
        val tid: String,
        @JsonProperty("partner_order_id") val partnerOrderId: String,
        @JsonProperty("partner_user_id") val partnerUserId: String,
        @JsonProperty("pg_token") val pgToken: String,
    )

    data class ApproveResponse(
        val tid: String,
        @JsonProperty("partner_order_id") val partnerOrderId: String,
        @JsonProperty("payment_method_type") val paymentMethodType: String,
    )

    fun ready(request: ReadyRequest): ReadyResponse {
        try {
            return restClient.post()
                .uri("$baseUrl/ready")
                .header("Authorization", "SECRET_KEY ${pgProperties.kakao.secretKey}")
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(ReadyResponse::class.java)
                ?: throw CustomException(ErrorCode.PG_CONNECTION_FAILED)
        } catch (e: RestClientException) {
            throw CustomException(ErrorCode.PG_CONNECTION_FAILED)
        }
    }

    fun approve(request: ApproveRequest): ApproveResponse {
        try {
            return restClient.post()
                .uri("$baseUrl/approve")
                .header("Authorization", "SECRET_KEY ${pgProperties.kakao.secretKey}")
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(ApproveResponse::class.java)
                ?: throw CustomException(ErrorCode.PG_PAYMENT_FAILED)
        } catch (e: RestClientException) {
            throw CustomException(ErrorCode.PG_PAYMENT_FAILED)
        }
    }
}
