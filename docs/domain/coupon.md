# 쿠폰 도메인 (Coupon)

> **구현 상태**: 구현 완료

## 개요

관리자가 쿠폰 정책(템플릿)을 생성하고, 사용자가 쿠폰을 발급받아 결제 시 할인을 적용받는 기능.
정액/정률 할인, 카테고리 조건, 최소 주문금액 조건, 발급 수량 제한을 지원한다.

## 핵심 엔티티

### CouponTemplate (쿠폰 정책)
관리자가 정의하는 쿠폰의 조건과 혜택.

| 필드 | 설명 |
|------|------|
| discountType | FIXED(정액) / RATE(정률) |
| discountValue | 정액: 차감 원 단위 / 정률: % 값 (1~100) |
| maxDiscountAmount | 정률 쿠폰 최대 할인 한도 (null=무제한) |
| minOrderAmount | 최소 주문금액 조건 (null=조건 없음) |
| categoryId | 적용 가능한 카테고리 ID (null=전체 적용) |
| totalQuantity | 발급 수량 한도 (null=무제한) |
| issuedCount | 현재 발급된 수량 (Optimistic Lock으로 경합 제어) |
| validFrom / validUntil | 쿠폰 유효 기간 |
| isActive | 관리자 활성화 여부 |

### UserCoupon (발급된 쿠폰)
사용자에게 발급된 쿠폰 인스턴스. `(userId, couponTemplateId)` UNIQUE 제약으로 중복 발급 방지.

| 상태 | 설명 |
|------|------|
| UNUSED | 발급 후 미사용 |
| USED | 결제 성공 시 전환 |
| EXPIRED | 만료 처리 (별도 스케줄러 활용 가능) |

## 비즈니스 규칙

### 쿠폰 발급 조건
1. 활성화된 쿠폰 (`isActive = true`)
2. 유효기간 내 (`validFrom ≤ 현재 ≤ validUntil`)
3. 동일 사용자 중복 발급 불가
4. 수량 한도 미소진 (`issuedCount < totalQuantity`)

### 결제 시 쿠폰 적용 조건
1. 본인 쿠폰 (`userId` 일치)
2. UNUSED 상태
3. 유효기간 내
4. 최소 주문금액 충족 (`order.totalAmount ≥ minOrderAmount`)
5. 카테고리 조건: 주문 내 해당 카테고리 상품이 1개 이상 포함

### 할인 계산
- **FIXED**: `min(discountValue, order.totalAmount)` — 주문금액 초과 할인 방지
- **RATE**: `order.totalAmount × discountValue / 100`, `maxDiscountAmount` 있으면 min 적용

### 결제 금액 구조
```
originalAmount (order.totalAmount) — 주문 원가 (불변)
discountAmount                      — 쿠폰 할인액
payment.amount = originalAmount - discountAmount  — 실제 결제액
```

## 동시성 처리

- **발급 수량 경합**: `CouponTemplate`에 `@Version` Optimistic Lock 적용
  - 동시 발급 충돌 → `ObjectOptimisticLockingFailureException` → `COUPON_QUANTITY_EXHAUSTED`
- **중복 발급 방지**: DB UNIQUE 제약 + 서비스 레이어 사전 확인 (이중 방어)
  - `DataIntegrityViolationException` → `COUPON_ALREADY_ISSUED`

## 유스케이스

### UC-1: 사용자가 쿠폰 발급
1. `POST /api/coupons/{templateId}/issue` 호출
2. 발급 조건 검증 (활성, 기간, 중복, 수량)
3. `issuedCount++` (Optimistic Lock)
4. `UserCoupon` 생성 (UNUSED)

### UC-2: 결제 시 쿠폰 적용
1. `POST /api/payments` 요청에 `couponId` 포함
2. `validateAndApplyCoupon()` — 조건 검증 + 할인 계산
3. `payment.amount = order.totalAmount - discountAmount`
4. 결제 성공 시 `markAsUsed()` — `UserCoupon.status = USED`

### UC-3: 관리자 쿠폰 관리
- 쿠폰 템플릿 생성 / 목록 조회 / 활성화 토글

## API 엔드포인트

### 사용자 API

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | `/api/coupons/{templateId}/issue` | 필요 | 쿠폰 발급 |
| GET | `/api/coupons/me` | 필요 | 내 쿠폰 목록 |

### 관리자 API

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | `/api/admin/coupons` | 필요(ADMIN) | 쿠폰 템플릿 생성 |
| GET | `/api/admin/coupons` | 필요(ADMIN) | 전체 쿠폰 목록 |
| PATCH | `/api/admin/coupons/{id}/toggle` | 필요(ADMIN) | 활성화/비활성화 토글 |
