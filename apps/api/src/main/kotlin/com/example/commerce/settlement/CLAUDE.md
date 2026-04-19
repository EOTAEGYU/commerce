# settlement 도메인

일별 매출 정산 집계를 담당한다. COMPLETED 결제 기준 순매출(쿠폰 할인 + 포인트 사용 차감 반영)을 집계한다.

## 폴더 구조

```
settlement/
├── controller/   # AdminSettlementController
├── service/      # SettlementService
├── repository/   # SettlementRepository
├── scheduler/    # SettlementScheduler (매일 00:05 자동 실행)
├── entity/       # Settlement, SettlementStatus
└── dto/          # SettlementCreateRequest, SettlementStatusUpdateRequest, SettlementResponse, SettlementStatsResponse
```

## Entity

### Settlement
- `settlementDate(LocalDate)` — UNIQUE, 날짜당 1건
- `status(SettlementStatus)` — var, 상태 변경 가능
- `orderCount(Int)` — 해당 날짜 완료 결제 건수
- `totalOrderAmount(Long)` — 총 주문금액 (실결제액 + 할인 + 포인트 역산)
- `totalDiscountAmount(Long)` — 총 쿠폰 할인액
- `totalPointAmount(Long)` — 총 포인트 사용액
- `totalNetAmount(Long)` — 순매출 (실결제액 합계 = Payment.amount 합계)
- `totalEarnedPoints(Long)` — 총 적립 포인트
- `confirmedAt(LocalDateTime?)` — var, CONFIRMED 전이 시 기록
- `paidAt(LocalDateTime?)` — var, PAID 전이 시 기록

### SettlementStatus
- `PENDING` → `CONFIRMED` → `PAID` 단방향 전이만 허용
- 역방향 전이 시 `SETTLEMENT_INVALID_STATUS` 예외

## 집계 로직 (Settlement.create)

```
totalOrderAmount  = Σ (payment.amount + payment.discountAmount + payment.pointAmount)
totalDiscountAmount = Σ payment.discountAmount
totalPointAmount  = Σ payment.pointAmount
totalNetAmount    = Σ payment.amount   ← 실결제액 (순매출)
totalEarnedPoints = Σ payment.earnedPoints
```

## 비즈니스 규칙

- 미래 날짜 정산 불가 (`SETTLEMENT_DATE_INVALID`)
- 동일 날짜 중복 정산 불가 (`SETTLEMENT_ALREADY_EXISTS`)
- 상태 전이: PENDING→CONFIRMED, CONFIRMED→PAID 만 허용 (`SETTLEMENT_INVALID_STATUS`)
- 스케줄러가 매일 00:05에 전날(yesterday) 정산을 자동 생성
- 이미 존재하는 날짜의 자동 정산 시도는 warn 없이 info 로그만 기록

## 주요 ErrorCode

- `SETTLEMENT_NOT_FOUND` — id로 정산 조회 실패
- `SETTLEMENT_ALREADY_EXISTS` — 동일 날짜 중복 생성 시도
- `SETTLEMENT_INVALID_STATUS` — 허용되지 않은 상태 전이
- `SETTLEMENT_DATE_INVALID` — 미래 날짜 정산 시도

## API 엔드포인트

모든 엔드포인트는 관리자 전용 (`/api/admin/settlements`), JWT 인증 필요.

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/admin/settlements` | 특정 날짜 정산 수동 생성 |
| GET | `/api/admin/settlements` | 정산 목록 조회 (from/to/page/size 파라미터) |
| GET | `/api/admin/settlements/stats` | 기간 집계 통계 (from/to 필수) |
| GET | `/api/admin/settlements/{id}` | 정산 단건 조회 |
| PATCH | `/api/admin/settlements/{id}/status` | 정산 상태 변경 |
