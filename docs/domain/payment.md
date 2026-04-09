# 결제 도메인 (Payment)

> **구현 상태**: 설계 예정

## 개요

주문에 대한 결제 처리 및 PG(Payment Gateway) 연동을 담당합니다.

## 결제 흐름

```
클라이언트                 서버                      PG사
   │                        │                         │
   │── POST /api/payments ─>│                         │
   │   { orderId, method }  │                         │
   │                        │── 결제 요청 ────────────>│
   │                        │<── 결제 결과 ────────────│
   │                        │                         │
   │                        │ 성공: Order.status = PAID│
   │                        │ 실패: Order.status 유지  │
   │<── { paymentResult } ──│                         │
```

## 비즈니스 규칙

- 결제는 `PENDING` 상태의 주문에만 가능하다.
- 결제 성공 시 Order 상태를 `PAID`로 변경한다.
- 결제 실패 시 재고를 복원하고 Order를 `CANCELLED`로 변경한다.
- 결제 수단: 카드, 계좌이체 등 (PG사 연동 후 확정).
- 환불 정책은 추후 별도 정의.

## 결제 상태

| 상태 | 설명 |
|------|------|
| `REQUESTED` | 결제 요청됨 |
| `COMPLETED` | 결제 완료 |
| `FAILED` | 결제 실패 |
| `REFUNDED` | 환불 완료 |

## 엔티티 구조 (예정)

```
Payment
├── orderId (FK, unique)
├── userId
├── amount (Long)
├── method (PaymentMethod)
├── status (PaymentStatus)
└── pgTransactionId (PG사 거래 ID)
```

## PG 연동 고려사항

- PG사 응답 검증 (위변조 방지)
- 중복 결제 방지 (idempotency key)
- 비동기 Webhook 처리 방안 검토
