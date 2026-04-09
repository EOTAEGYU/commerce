# 주문 도메인 (Order)

> **구현 상태**: 구현 완료

## 개요

주문 생성, 상태 관리, 취소를 담당합니다. 주문은 장바구니 전체를 기반으로 생성됩니다.

## 주문 상태 전이

```
PENDING (결제 대기)
    │
    ├── 결제 성공 → PAID (결제 완료)
    │                   │
    │                   ├── 배송 시작 → SHIPPING (배송 중)
    │                   │                   │
    │                   │                   └── 배송 완료 → DELIVERED (배송 완료)
    │                   │
    │                   └── 취소 요청 → CANCELLED (취소)
    │
    ├── 결제 실패 → CANCELLED (취소) + 재고 복원
    │
    └── 10분 경과 (미결제) → CANCELLED (취소) + 재고 복원 [스케줄러 자동 처리]
```

## 비즈니스 규칙

- 주문 생성 시 재고를 즉시 차감한다 (Pessimistic Lock).
- 주문 취소 시 재고를 복원한다.
- **취소 가능 상태**: `PENDING`, `PAID`만 취소 가능. `SHIPPING` 이후는 불가.
- 주문 금액은 생성 시점의 장바구니 가격으로 고정된다.
- `PENDING` 상태로 10분 경과 시 스케줄러가 자동으로 `CANCELLED` 처리한다.
- 재고 부족 시 전체 주문 실패 (부분 성공 없음).

## 주요 유스케이스

### 1. 주문 생성 (`POST /api/orders`)
1. 장바구니 조회 (비어있으면 `CART_EMPTY`)
2. 각 항목별 재고 확인 및 차감 (Pessimistic Lock, `OUT_OF_STOCK`)
3. OrderItem 스냅샷 생성 (상품명, 옵션정보, 가격 고정)
4. Order 저장 → 상태: `PENDING`
5. 장바구니 비우기

### 2. 주문 취소 (`POST /api/orders/{id}/cancel`)
1. 본인 주문 확인
2. 취소 가능 상태 확인 (`PENDING` 또는 `PAID`)
3. 재고 복원 (Pessimistic Lock)
4. 상태 변경 → `CANCELLED`

### 3. 주문 만료 (스케줄러, 1분마다 실행)
- `PENDING` 상태이면서 `createdAt`이 10분 이상 경과한 주문을 찾아
- 재고 복원 후 `CANCELLED` 처리

## 엔티티 구조

```
Order
├── userId
├── status (OrderStatus)       ← PENDING / PAID / SHIPPING / DELIVERED / CANCELLED
├── totalAmount (Long)
└──< OrderItem (N)
        ├── productId           ← 참조용
        ├── productOptionId     ← 참조용
        ├── productName         ← 주문 시점 스냅샷
        ├── optionInfo          ← "M / 블랙" 형태 스냅샷
        ├── price               ← 주문 시점 스냅샷
        └── quantity
```

## API 엔드포인트

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/orders` | 필요 | 주문 생성 (장바구니 전체 기반) |
| GET | `/api/orders` | 필요 | 내 주문 목록 (최신순) |
| GET | `/api/orders/{id}` | 필요 | 주문 상세 |
| POST | `/api/orders/{id}/cancel` | 필요 | 주문 취소 |

## 에러 코드

| 코드 | 발생 상황 |
|------|----------|
| `CART_EMPTY` | 빈 장바구니로 주문 생성 시도 |
| `OUT_OF_STOCK` | 재고 부족 시 |
| `ORDER_NOT_FOUND` | 존재하지 않는 주문 조회/취소 시도 |
| `ORDER_NOT_OWNED` | 다른 회원의 주문 조회/취소 시도 |
| `ORDER_CANNOT_CANCEL` | SHIPPING/DELIVERED/CANCELLED 상태에서 취소 시도 |
