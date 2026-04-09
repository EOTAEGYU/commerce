# 결제 도메인 (Payment)

> **구현 상태**: 구현 완료

## 개요

주문에 대한 결제 처리를 담당합니다. 현재는 Mock PG로 구현하며, 실제 PG 연동은 추후 교체합니다.

## 결제 흐름

```
클라이언트                         서버
   │                                │
   │── POST /api/payments ─────────>│
   │   { orderId, method,           │
   │     simulateFailure }          │
   │                                │ 1. PENDING 주문 확인
   │                                │ 2. 중복 결제 확인
   │                                │ 3. Payment 생성 (REQUESTED)
   │                                │ 4. Mock PG 호출
   │                                │    ├── 성공 → Order = PAID, Payment = COMPLETED
   │                                │    └── 실패 → 재고 복원, Order = CANCELLED, Payment = FAILED
   │<── { paymentResult } ──────────│
```

## 비즈니스 규칙

- 결제는 `PENDING` 상태의 주문에만 가능하다 (`ORDER_NOT_PAYABLE`).
- 이미 결제 레코드가 존재하면 중복 결제로 거부한다 (`ORDER_ALREADY_PAID`).
- 결제 성공 시 Order 상태를 `PAID`로 변경한다.
- 결제 실패 시 재고를 복원하고 Order를 `CANCELLED`로 변경한다.
- `PENDING` 상태가 10분 초과 시 스케줄러가 자동으로 만료 처리한다 (order 도메인 담당).

## Mock PG 동작

실제 PG 연동 없이 `simulateFailure` 파라미터로 성공/실패를 제어한다.

```json
// 결제 성공 요청
{ "orderId": 1, "method": "CARD", "simulateFailure": false }

// 결제 실패 시뮬레이션
{ "orderId": 1, "method": "CARD", "simulateFailure": true }
```

## 결제 상태

| 상태 | 설명 |
|------|------|
| `REQUESTED` | 결제 요청됨 (처리 중) |
| `COMPLETED` | 결제 완료 |
| `FAILED` | 결제 실패 |
| `REFUNDED` | 환불 완료 |

## 결제 수단

| 수단 | 설명 |
|------|------|
| `CARD` | 신용/체크카드 |
| `BANK_TRANSFER` | 계좌이체 |

## 엔티티 구조

```
Payment
├── orderId (FK, UNIQUE)   ← 주문당 1건
├── userId
├── amount (Long)
├── method (PaymentMethod)
├── status (PaymentStatus)
└── pgTransactionId        ← 성공 시 PG 거래 ID 저장 (Mock: null)
```

## API 엔드포인트

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/payments` | 필요 | 결제 요청 |
| GET | `/api/payments/{orderId}` | 필요 | 결제 상태 조회 |

## 에러 코드

| 코드 | 발생 상황 |
|------|----------|
| `PAYMENT_NOT_FOUND` | orderId로 결제 내역 조회 실패 |
| `ORDER_NOT_PAYABLE` | PENDING이 아닌 주문에 결제 요청 |
| `ORDER_ALREADY_PAID` | 이미 결제 레코드가 존재하는 주문에 재결제 시도 |
