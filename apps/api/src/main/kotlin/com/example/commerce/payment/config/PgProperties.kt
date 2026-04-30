package com.example.commerce.payment.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pg")
data class PgProperties(
    val kakao: KakaoProperties,
    val toss: TossProperties,
) {
    data class KakaoProperties(
        val secretKey: String,
        val cid: String,
    )

    data class TossProperties(
        val secretKey: String,
    )
}
