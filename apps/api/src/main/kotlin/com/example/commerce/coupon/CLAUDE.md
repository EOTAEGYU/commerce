# coupon 도메인

쿠폰 템플릿 관리 및 사용자 쿠폰 발급·사용을 담당한다.
관리자가 쿠폰 템플릿을 생성하고, 사용자는 템플릿을 기반으로 쿠폰을 발급받아 결제 시 적용한다.

## 폴더 구조

```
coupon/
├── controller/   # CouponController, AdminCouponController
├── service/      # CouponService
├── repository/   # CouponTemplateRepository, UserCouponRepository
├── entity/       # CouponTemplate, UserCoupon, DiscountType, UserCouponStatus
└── dto/          # CouponTemplateCreateRequest, CouponTemplateResponse, UserCouponResponse
```

## Entity

### CouponTemplate
- `name(String)`, `discountType(DiscountType)`, `discountValue(Long)` — 기본 쿠폰 정보
- `maxDiscountAmount(Long?)` — RATE 타입 최대 할인 한도 (null이면 무제한)
- `minOrderAmount(Long?)` — 최소 주문금액 조건 (null이면 조건 없음)
- `categoryId(Long?)` — 특정 카테고리 상품이 포함된 주문에만 적용 (null이면 전체 적용)
- `totalQuantity(Int?)` — 발급 수량 한도 (null이면 무제한)
- `issuedCount(Int, var)` — 현재까지 발급된 수량
- `validFrom`, `validUntil(LocalDateTime)` — 쿠폰 유효 기간
- `isActive(Boolean, var)` — 관리자 활성화/비활성화 여부
- `version(Long)` — Optimistic Lock (`@Version`) — 동시 발급 시 수량 경합 제어

### UserCoupon
- `userId(Long)`, `couponTemplateId(Long)` — 발급 대상 식별
- `status(UserCouponStatus, var)` — UNUSED / USED / EXPIRED
- `usedOrderId(Long?, var)` — 사용된 주문 ID
- `usedAt(LocalDateTime?, var)` — 사용 시각
- `(user_id, coupon_template_id)` UniqueConstraint — 동일 쿠폰 중복 발급 방지

### DiscountType
- `FIXED` — 정액 할인 (discountValue 원 차감)
- `RATE` — 정률 할인 (discountValue% 할인, maxDiscountAmount 상한 적용 가능)

### UserCouponStatus
- `UNUSED` — 발급 후 미사용
- `USED` — 결제 완료 시 전환
- `EXPIRED` — 만료 처리 (스케줄러 등 별도 처리 시 활용)

## 비즈니스 규칙

### 쿠폰 발급 (issueCoupon)
1. 템플릿 존재 확인 → `COUPON_NOT_FOUND`
2. 활성 상태 확인 → `COUPON_NOT_ACTIVE`
3. 유효기간 확인 (validFrom~validUntil) → `COUPON_EXPIRED`
4. 중복 발급 확인 → `COUPON_ALREADY_ISSUED`
5. 수량 한도 확인 (totalQuantity != null && issuedCount >= totalQuantity) → `COUPON_QUANTITY_EXHAUSTED`
6. Optimistic Lock: `issuedCount++` 후 save — `ObjectOptimisticLockingFailureException` 발생 시 `COUPON_QUANTITY_EXHAUSTED`
7. UserCoupon save 시 `DataIntegrityViolationException` → `COUPON_ALREADY_ISSUED` (DB 레벨 중복 방어)

### 쿠폰 검증 및 할인 계산 (validateAndApplyCoupon)
- `PaymentService.requestPayment()`에서 호출 (결제 시)
- 소유권 확인 → `COUPON_NOT_OWNED`
- UNUSED 상태 확인 → `COUPON_ALREADY_USED`
- 유효기간 확인 → `COUPON_EXPIRED`
- 최소 주문금액 확인 → `COUPON_MIN_AMOUNT_NOT_MET`
- 카테고리 조건: order.items의 productId로 상품 조회 → 해당 categoryId 상품 1개 이상 필요 → `COUPON_CATEGORY_NOT_MET`
- 할인 계산:
  - FIXED: `min(discountValue, order.totalAmount)`
  - RATE: `order.totalAmount * discountValue / 100`, maxDiscountAmount 있으면 min 적용
- 반환값: `Pair<Long, Long>` = `discountAmount to userCouponId`

### 쿠폰 사용 처리 (markAsUsed)
- 결제 성공(`simulateFailure = false`) 후에만 호출
- status = USED, usedOrderId, usedAt 설정

## 결제 연동 (PaymentService 수정)

- `PaymentRequest`에 `couponId: Long?` 추가
- `Payment`에 `discountAmount: Long`, `couponId: Long?` 추가
- `PaymentResponse`에 `discountAmount: Long`, `originalAmount: Long` 추가
- `PaymentResponse.from(payment, originalAmount)` 시그니처 변경
- `getPayment()`에서 `originalAmount = payment.amount + payment.discountAmount` 계산

## API 엔드포인트

### 사용자 API (`/api/coupons`)

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | /api/coupons/{templateId}/issue | 필요 | 쿠폰 발급 |
| GET | /api/coupons/me | 필요 | 내 쿠폰 목록 조회 |

### 관리자 API (`/api/admin/coupons`)

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | /api/admin/coupons | 필요(ADMIN) | 쿠폰 템플릿 생성 |
| GET | /api/admin/coupons | 필요(ADMIN) | 전체 쿠폰 템플릿 조회 |
| PATCH | /api/admin/coupons/{id}/toggle | 필요(ADMIN) | 쿠폰 활성화/비활성화 토글 |

## 주요 ErrorCode

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `COUPON_NOT_FOUND` | 404 | 쿠폰을 찾을 수 없습니다 |
| `COUPON_NOT_ACTIVE` | 400 | 비활성화된 쿠폰입니다 |
| `COUPON_ALREADY_ISSUED` | 409 | 이미 발급받은 쿠폰입니다 |
| `COUPON_QUANTITY_EXHAUSTED` | 409 | 쿠폰 수량이 소진되었습니다 |
| `COUPON_EXPIRED` | 400 | 만료된 쿠폰입니다 |
| `COUPON_ALREADY_USED` | 409 | 이미 사용된 쿠폰입니다 |
| `COUPON_NOT_OWNED` | 403 | 본인의 쿠폰이 아닙니다 |
| `COUPON_MIN_AMOUNT_NOT_MET` | 400 | 최소 주문금액을 충족하지 못했습니다 |
| `COUPON_CATEGORY_NOT_MET` | 400 | 쿠폰 적용 카테고리 조건을 충족하지 못했습니다 |

## 주의사항

- `CouponTemplate`의 nullable 프로퍼티(`totalQuantity`, `minOrderAmount`, `maxDiscountAmount`)는 Service에서 로컬 변수에 담아 smart cast 오류 방지
- Optimistic Lock(`@Version`)은 동시 발급 경합만 처리 — 단일 요청 흐름에서는 영향 없음
- `validateAndApplyCoupon`은 `@Transactional` 없이 readOnly 트랜잭션에서 실행 (PaymentService 트랜잭션 내 호출)
- `markAsUsed`는 별도 `@Transactional`로 선언 — 결제 성공 여부 확인 후 호출
