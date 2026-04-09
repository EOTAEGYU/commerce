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

## 상품 (Product) — 추가 예정

| 코드 | HTTP 상태 | 메시지 | 발생 상황 |
|------|----------|--------|----------|
| `PRODUCT_NOT_FOUND` | 404 | 존재하지 않는 상품입니다. | productId로 상품 조회 실패 |
| `OUT_OF_STOCK` | 409 | 재고가 부족합니다. | 재고보다 많은 수량 주문/장바구니 추가 |
| `UNAUTHORIZED_PRODUCT_ACCESS` | 403 | 상품에 대한 접근 권한이 없습니다. | 다른 판매자 상품 수정/삭제 시도 |

## 사용 방법

```kotlin
// 예외 던지기
throw CustomException(ErrorCode.USER_NOT_FOUND)

// 새 에러 코드 추가 (ErrorCode.kt에)
NEW_ERROR_CODE(HttpStatus.BAD_REQUEST, "에러 메시지"),
```

`GlobalExceptionHandler`가 `CustomException`을 잡아 자동으로 `ApiResponse.error(errorCode)`로 변환합니다.
