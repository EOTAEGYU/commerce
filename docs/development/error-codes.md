# 에러 코드 목록

> 파일 위치: `src/main/kotlin/com/example/commerce/common/ErrorCode.kt`

## 공통

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류가 발생했습니다. | 예상치 못한 예외 발생 시 |
| `INVALID_INPUT` | 400 | 잘못된 입력값입니다. | Bean Validation 실패 시 |
| `ENTITY_NOT_FOUND` | 404 | 요청한 리소스를 찾을 수 없습니다. | 일반 리소스 조회 실패 |

## 회원 (User)

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `DUPLICATE_EMAIL` | 409 | 이미 사용 중인 이메일입니다. | 이미 가입된 이메일로 회원가입 시도 |
| `USER_NOT_FOUND` | 404 | 존재하지 않는 회원입니다. | userId로 회원 조회 실패 |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호가 올바르지 않습니다. | 로그인 시 이메일 없거나 비밀번호 불일치 |

## 카테고리 (Category)

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `CATEGORY_NOT_FOUND` | 404 | 존재하지 않는 카테고리입니다. | categoryId로 카테고리 조회 실패 |
| `CATEGORY_HAS_CHILDREN` | 409 | 하위 카테고리가 존재합니다. | 소분류가 있는 대분류 삭제 시도 |
| `CATEGORY_IN_USE` | 409 | 상품이 등록된 카테고리입니다. | 상품이 속한 카테고리 삭제 시도 |

## 상품 (Product)

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `PRODUCT_NOT_FOUND` | 404 | 존재하지 않는 상품입니다. | productId로 상품 조회 실패 |
| `PRODUCT_OPTION_NOT_FOUND` | 404 | 존재하지 않는 상품 옵션입니다. | optionId로 옵션 조회 실패 |
| `OUT_OF_STOCK` | 409 | 재고가 부족합니다. | 재고보다 많은 수량 주문/장바구니 추가 |

## 장바구니 (Cart)

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `CART_ITEM_NOT_FOUND` | 404 | 존재하지 않는 장바구니 항목입니다. | cartItemId로 항목 조회 실패 |
| `CART_ITEM_NOT_OWNED` | 403 | 본인의 장바구니 항목이 아닙니다. | 다른 회원의 장바구니 항목 수정/삭제 시도 |

## 주문 (Order)

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `ORDER_NOT_FOUND` | 404 | 존재하지 않는 주문입니다. | orderId로 주문 조회 실패 |
| `ORDER_NOT_OWNED` | 403 | 본인의 주문이 아닙니다. | 다른 회원의 주문 조회/취소 시도 |
| `ORDER_CANNOT_CANCEL` | 409 | 취소할 수 없는 주문 상태입니다. | SHIPPING/DELIVERED/CANCELLED 상태 주문 취소 시도 |
| `CART_EMPTY` | 400 | 장바구니가 비어 있습니다. | 빈 장바구니로 주문 생성 시도 |

## 결제 (Payment)

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `PAYMENT_NOT_FOUND` | 404 | 존재하지 않는 결제 내역입니다. | orderId로 결제 조회 실패 |
| `ORDER_NOT_PAYABLE` | 409 | 결제할 수 없는 주문 상태입니다. | PENDING이 아닌 주문에 결제 요청 |
| `ORDER_ALREADY_PAID` | 409 | 이미 결제된 주문입니다. | 중복 결제 시도 |

## 주문 항목 (OrderItem)

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `ORDER_ITEM_NOT_FOUND` | 404 | 존재하지 않는 주문 항목입니다. | orderItemId로 항목 조회 실패 |

## 리뷰 (Review)

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `REVIEW_NOT_FOUND` | 404 | 존재하지 않는 리뷰입니다. | reviewId로 리뷰 조회 실패 |
| `REVIEW_ALREADY_EXISTS` | 409 | 이미 리뷰를 작성했습니다. | 동일 OrderItem에 중복 리뷰 작성 시도 |
| `REVIEW_NOT_OWNED` | 403 | 본인의 리뷰가 아닙니다. | 다른 회원의 리뷰 수정/삭제 시도 |
| `ORDER_NOT_DELIVERED` | 400 | 배송 완료된 주문에만 리뷰를 작성할 수 있습니다. | DELIVERED 상태가 아닌 주문의 OrderItem에 리뷰 작성 |
| `INVALID_RATING` | 400 | 별점은 0.5 단위로 0.5 ~ 5.0 사이여야 합니다. | 1.3, 2.7 등 0.5 단위가 아닌 별점 입력 |

## 좋아요 (Like)

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `LIKE_NOT_FOUND` | 404 | 존재하지 않는 좋아요입니다. | 좋아요 취소 시 해당 좋아요 없음 |

## 쿠폰 (Coupon)

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `COUPON_NOT_FOUND` | 404 | 쿠폰을 찾을 수 없습니다. | 쿠폰 ID로 조회 실패 |
| `COUPON_NOT_ACTIVE` | 400 | 비활성화된 쿠폰입니다. | isActive=false 쿠폰 발급 시도 |
| `COUPON_ALREADY_ISSUED` | 409 | 이미 발급받은 쿠폰입니다. | 동일 쿠폰 중복 발급 시도 |
| `COUPON_QUANTITY_EXHAUSTED` | 409 | 쿠폰 수량이 소진되었습니다. | 발급 수량 한도 초과 또는 동시 발급 경합 |
| `COUPON_EXPIRED` | 400 | 만료된 쿠폰입니다. | 유효기간 외 발급/사용 시도 |
| `COUPON_ALREADY_USED` | 409 | 이미 사용된 쿠폰입니다. | USED/EXPIRED 상태 쿠폰 결제 적용 시도 |
| `COUPON_NOT_OWNED` | 403 | 본인의 쿠폰이 아닙니다. | 다른 회원의 UserCoupon 사용 시도 |
| `COUPON_MIN_AMOUNT_NOT_MET` | 400 | 최소 주문금액을 충족하지 못했습니다. | 주문금액 < minOrderAmount |
| `COUPON_CATEGORY_NOT_MET` | 400 | 쿠폰 적용 카테고리 조건을 충족하지 못했습니다. | 주문에 해당 카테고리 상품 없음 |

## 사용 방법

```kotlin
// 예외 던지기
throw CustomException(ErrorCode.USER_NOT_FOUND)

// 새 에러 코드 추가 (ErrorCode.kt에)
NEW_ERROR_CODE(HttpStatus.BAD_REQUEST, "에러 메시지"),
```

`GlobalExceptionHandler`가 `CustomException`을 잡아 자동으로 `ApiResponse.error(errorCode)`로 변환합니다.
