# payment 도메인

주문에 대한 결제 처리를 담당한다. Mock PG 방식으로 구현되어 있으며 실제 PG 연동은 추후 교체한다.

## 폴더 구조

```
payment/
├── controller/   # PaymentController
├── service/      # PaymentService
├── repository/   # PaymentRepository
├── entity/       # Payment, PaymentMethod, PaymentStatus
└── dto/          # PaymentRequest, PaymentResponse
```

## Entity

### Payment
- `orderId(Long)` — UNIQUE, 주문당 1건
- `userId(Long)` — 결제 요청자
- `amount(Long)` — 결제 금액 (주문 totalAmount와 동일)
- `method(PaymentMethod)` — 결제 수단
- `status(PaymentStatus)` — var, 결제 상태 변경 가능
- `pgTransactionId(String?)` — var, 성공 시 UUID 저장

### PaymentMethod
- `CARD`, `BANK_TRANSFER`

### PaymentStatus
- `REQUESTED` → `COMPLETED` (성공) 또는 `FAILED` (실패)
- `REFUNDED` (추후 환불 처리용)

## Mock PG 동작

`simulateFailure` 파라미터로 성공/실패를 제어한다.

```kotlin
// 성공 흐름
simulateFailure = false
→ order.status = PAID
→ payment.status = COMPLETED
→ payment.pgTransactionId = UUID

// 실패 흐름
simulateFailure = true
→ 각 OrderItem의 stock 복원 (Pessimistic Lock)
→ order.status = CANCELLED
→ payment.status = FAILED
```

## 비즈니스 규칙

- 결제 대상 주문은 반드시 `PENDING` 상태 (`ORDER_NOT_PAYABLE`)
- `COMPLETED` 결제가 이미 존재하면 중복 결제 거부 (`ORDER_ALREADY_PAID`)
- 결제 실패 시 재고 복원은 `ProductOptionRepository.findByIdWithLock` 사용
- 10분 미결제 주문 자동 만료는 `order/scheduler/OrderExpirationScheduler` 담당

## 주요 ErrorCode

- `PAYMENT_NOT_FOUND` — orderId로 결제 조회 실패
- `ORDER_NOT_PAYABLE` — PENDING 아닌 주문에 결제 요청
- `ORDER_ALREADY_PAID` — 중복 결제 시도

## API 엔드포인트

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/payments` | 필요 | 결제 요청 |
| GET | `/api/payments/{orderId}` | 필요 | 결제 상태 조회 |
