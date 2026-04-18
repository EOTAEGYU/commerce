package com.example.commerce.common

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    // User
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),
    CATEGORY_HAS_CHILDREN(HttpStatus.CONFLICT, "하위 카테고리가 존재합니다."),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "상품이 등록된 카테고리입니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    PRODUCT_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품 옵션입니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),

    // Cart
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장바구니 항목입니다."),
    CART_ITEM_NOT_OWNED(HttpStatus.FORBIDDEN, "본인의 장바구니 항목이 아닙니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다."),
    ORDER_NOT_OWNED(HttpStatus.FORBIDDEN, "본인의 주문이 아닙니다."),
    ORDER_CANNOT_CANCEL(HttpStatus.CONFLICT, "취소할 수 없는 주문 상태입니다."),
    CART_EMPTY(HttpStatus.BAD_REQUEST, "장바구니가 비어 있습니다."),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 결제 내역입니다."),
    ORDER_NOT_PAYABLE(HttpStatus.CONFLICT, "결제할 수 없는 주문 상태입니다."),
    ORDER_ALREADY_PAID(HttpStatus.CONFLICT, "이미 결제된 주문입니다."),

    // Order Item
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문 항목입니다."),

    // Review
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 리뷰입니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 리뷰를 작성했습니다."),
    REVIEW_NOT_OWNED(HttpStatus.FORBIDDEN, "본인의 리뷰가 아닙니다."),
    ORDER_NOT_DELIVERED(HttpStatus.BAD_REQUEST, "배송 완료된 주문에만 리뷰를 작성할 수 있습니다."),
    INVALID_RATING(HttpStatus.BAD_REQUEST, "별점은 0.5 단위로 0.5 ~ 5.0 사이여야 합니다."),

    // Coupon
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다"),
    COUPON_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "비활성화된 쿠폰입니다"),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급받은 쿠폰입니다"),
    COUPON_QUANTITY_EXHAUSTED(HttpStatus.CONFLICT, "쿠폰 수량이 소진되었습니다"),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 쿠폰입니다"),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다"),
    COUPON_NOT_OWNED(HttpStatus.FORBIDDEN, "본인의 쿠폰이 아닙니다"),
    COUPON_MIN_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "최소 주문금액을 충족하지 못했습니다"),
    COUPON_CATEGORY_NOT_MET(HttpStatus.BAD_REQUEST, "쿠폰 적용 카테고리 조건을 충족하지 못했습니다"),
}
