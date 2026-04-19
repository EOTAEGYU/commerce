# point 도메인

사용자 포인트 잔액 관리 및 이력 추적을 담당한다.
리뷰 작성과 구매 완료 시 포인트가 적립되며, 결제 시 포인트를 차감할 수 있다.

## 폴더 구조

```
point/
├── controller/   # PointController
├── service/      # PointService
├── repository/   # UserPointRepository, PointHistoryRepository
├── entity/       # UserPoint, PointHistory, PointHistoryType
└── dto/          # PointBalanceResponse, PointHistoryResponse
```

## Entity

### UserPoint
- `userId(Long)` — UNIQUE, 사용자당 1개
- `balance(Long, var)` — 현재 잔액
- `version(Long)` — Optimistic Lock (`@Version`) — 동시 차감 경합 제어

### PointHistory
- `userId(Long)`, `type(PointHistoryType)`, `amount(Long)` — 항상 양수
- `balance(Long)` — 거래 후 잔액 스냅샷
- `relatedId(Long?)` — orderId 또는 reviewId
- `description(String?)` — 이력 설명

### PointHistoryType
- `EARN_REVIEW` — 리뷰 작성 적립 (300포인트 고정)
- `EARN_PURCHASE` — 구매 적립 (결제금액의 1%, 소수점 버림)
- `USE_PAYMENT` — 결제 사용 (차감)
- `REFUND_PAYMENT` — 결제 취소 환불

## 비즈니스 규칙

### 포인트 사용 (validateAndUsePoints)
- `pointAmount <= 0` → 0 반환 (포인트 미사용)
- `pointAmount < 1000` → `POINT_BELOW_MINIMUM` (최소 1,000 포인트)
- `pointAmount > order.totalAmount * 50 / 100` → `POINT_EXCEEDS_MAXIMUM` (최대 50%)
- `userPoint.balance < pointAmount` → `POINT_INSUFFICIENT`

### 포인트 환불 (refundPoints)
- USE_PAYMENT 이력이 없으면 아무 동작 없이 return
- 이력이 있으면 해당 amount만큼 balance에 복원 후 REFUND_PAYMENT 이력 저장

### getOrCreateUserPoint
- UserPoint가 없으면 자동 생성 (balance=0)으로 저장 후 반환

## 타 도메인 연동

### PaymentService
- 결제 요청 시 `validateAndUsePoints()` → 포인트 차감
- 결제 성공 시 `earnPurchasePoints()` → 구매 포인트 적립, `payment.earnedPoints` 업데이트
- 결제 실패 시 `refundPoints()` → 포인트 환불

### ReviewService
- 리뷰 저장 후 `earnReviewPoints()` → 300포인트 적립

## API 엔드포인트

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| GET | /api/points/me | 필요 | 포인트 잔액 조회 |
| GET | /api/points/history | 필요 | 포인트 이력 조회 (페이징, 기본 size=20) |

## 주요 ErrorCode

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `POINT_INSUFFICIENT` | 400 | 포인트 잔액이 부족합니다 |
| `POINT_BELOW_MINIMUM` | 400 | 최소 1,000 포인트 이상 사용해야 합니다 |
| `POINT_EXCEEDS_MAXIMUM` | 400 | 결제금액의 50%를 초과하여 사용할 수 없습니다 |
| `POINT_INVALID_AMOUNT` | 400 | 유효하지 않은 포인트 금액입니다 |

## 주의사항

- `UserPoint`는 Optimistic Lock(`@Version`)으로 동시 차감 경합 제어
- `earnPoints()`는 `@Transactional`이지만 내부에서 `getOrCreateUserPoint()`를 호출한다 — self-invocation 문제 없음 (별도 트랜잭션 메서드 X, 같은 트랜잭션 내 비-트랜잭션 private 메서드 호출)
- `earnPurchasePoints`, `earnReviewPoints`는 각각 `@Transactional`로 선언하되 내부에서 `earnPoints()`를 직접 호출 — Spring AOP 우회 없음
- `PaymentResponse`의 `originalAmount` = `payment.amount + payment.discountAmount + payment.pointAmount`
