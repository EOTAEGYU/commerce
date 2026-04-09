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
}
