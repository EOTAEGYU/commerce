# 정산 도메인 (Settlement)

> **구현 상태**: 구현 완료

## 개요

일별 COMPLETED 결제를 집계하여 순매출(쿠폰 할인 + 포인트 사용 차감 반영)을 기록하는 관리자 전용 도메인.
스케줄러가 매일 00:05에 전일 정산을 자동 생성하며, 관리자가 수동으로 생성하거나 상태를 관리할 수 있다.

## 비즈니스 규칙

- 정산 단위는 **1일(LocalDate)**이며, 날짜당 1개의 정산 레코드만 존재한다.
- **미래 날짜 정산 불가** (`SETTLEMENT_DATE_INVALID`)
- **동일 날짜 중복 정산 불가** (`SETTLEMENT_ALREADY_EXISTS`)
- 상태 전이는 `PENDING → CONFIRMED → PAID` 단방향만 허용 (`SETTLEMENT_INVALID_STATUS`)
- 스케줄러(매일 00:05)가 전날 정산을 자동 생성하며, 이미 존재하면 info 로그만 기록(예외 무시)
- 정산 대상: `Payment.status = COMPLETED` + `Payment.createdAt` in 해당 날짜 00:00 ~ 23:59:59

## 설계 결정

### 집계 로직 (Settlement.create)

```
totalOrderAmount   = Σ (payment.amount + payment.discountAmount + payment.pointAmount)
totalDiscountAmount = Σ payment.discountAmount
totalPointAmount   = Σ payment.pointAmount
totalNetAmount     = Σ payment.amount        ← 실결제액 (순매출)
totalEarnedPoints  = Σ payment.earnedPoints
```

- `payment.amount`는 이미 쿠폰/포인트가 차감된 실결제액이므로 `totalNetAmount = Σ amount`이 순매출

### 상태 전이 규칙

| 현재 상태 | 전이 가능 상태 |
|-----------|---------------|
| PENDING   | CONFIRMED     |
| CONFIRMED | PAID          |
| PAID      | (없음)        |

역방향 및 임의 전이 시 `SETTLEMENT_INVALID_STATUS` 예외 발생

### getSettlements 기본 기간

`from`, `to` 파라미터 없이 조회 시 기본값: 최근 3개월 (`now().minusMonths(3)` ~ `now()`)

## 주요 유스케이스

### 1. 정산 자동 생성 (SettlementScheduler)
- 매일 00:05에 어제 날짜로 `createSettlement(yesterday)` 호출
- `SETTLEMENT_ALREADY_EXISTS` 예외는 조용히 무시 (재실행 안전)

### 2. 정산 수동 생성 (createSettlement)
1. 미래 날짜 여부 확인 → `SETTLEMENT_DATE_INVALID`
2. 해당 날짜 정산 중복 확인 → `SETTLEMENT_ALREADY_EXISTS`
3. `PaymentRepository`에서 COMPLETED 결제 목록 조회
4. `Settlement.create(date, payments)` 집계 → 저장

### 3. 정산 상태 변경 (updateStatus)
1. 정산 조회 → `SETTLEMENT_NOT_FOUND`
2. 상태 전이 유효성 검증 → `SETTLEMENT_INVALID_STATUS`
3. 상태 변경 + `confirmedAt` / `paidAt` 타임스탬프 기록 (JPA dirty checking)

### 4. 통계 리포트 (getStats)
- 기간 내 Settlement 목록 조회 후 in-memory 집계
- `avgDailyNetAmount = totalNetAmount / settledDays` (settledDays=0이면 0)

## 파일 구조

```
settlement/
├── entity/
│   ├── Settlement.kt          # JPA Entity, companion object { fun create(...) } 포함
│   └── SettlementStatus.kt    # PENDING / CONFIRMED / PAID
├── repository/
│   └── SettlementRepository.kt
├── service/
│   └── SettlementService.kt
├── controller/
│   └── AdminSettlementController.kt   # /api/admin/settlements
├── scheduler/
│   └── SettlementScheduler.kt         # @Scheduled(cron = "0 5 0 * * *")
├── dto/
│   ├── SettlementCreateRequest.kt
│   ├── SettlementStatusUpdateRequest.kt
│   ├── SettlementResponse.kt
│   └── SettlementStatsResponse.kt
└── CLAUDE.md
```

## API 엔드포인트

모든 엔드포인트는 관리자 전용, JWT 인증 필요.

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/admin/settlements` | **필요** (ADMIN) | 특정 날짜 정산 수동 생성 |
| GET | `/api/admin/settlements` | **필요** (ADMIN) | 정산 목록 조회 (기간 필터, 페이징) |
| GET | `/api/admin/settlements/stats` | **필요** (ADMIN) | 기간 집계 통계 (from/to 필수) |
| GET | `/api/admin/settlements/{id}` | **필요** (ADMIN) | 정산 단건 조회 |
| PATCH | `/api/admin/settlements/{id}/status` | **필요** (ADMIN) | 정산 상태 변경 |
