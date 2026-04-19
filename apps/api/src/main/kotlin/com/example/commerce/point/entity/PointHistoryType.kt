package com.example.commerce.point.entity

enum class PointHistoryType(val description: String) {
    EARN_REVIEW("리뷰 작성 적립"),
    EARN_PURCHASE("구매 적립"),
    USE_PAYMENT("결제 사용"),
    REFUND_PAYMENT("결제 취소 환불"),
}
