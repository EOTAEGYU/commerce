# 백엔드 도메인 주의사항

코드에서 바로 읽히지 않는 비자명한 규칙만 기록한다.

### common
- `ErrorCode` 추가 시 `HttpStatus` + 한국어 메시지 함께 정의
- `GlobalExceptionHandler`는 직접 수정하지 않음

### user
- 로그인은 **username(아이디) + 비밀번호** (이메일 로그인 아님)

### product
- 재고는 `ProductOption`에서 관리, 변경 시 `findByIdWithLock()` (Pessimistic Lock) 필수

### cart
- 동일 `productOptionId` → 수량 합산 (새 row 아님), 주문 완료 후 `cart.items.clear()`

### order
- PENDING 10분 초과 → `OrderExpirationScheduler` 자동 CANCELLED + 재고 복원
- 스케줄러: `findExpiredOrdersWithItems` (JOIN FETCH) — LazyInitializationException 방지

### payment
- 카카오페이(`ready→approve`), 토스페이먼츠(`ready→confirm`) 2-step 흐름
- 중간 세션: `PgPaymentSession` Entity, 결제금액 = `totalAmount - couponDiscount - pointUsed`

### coupon
- 발급 시 `issuedCount++` → Optimistic Lock(`@Version`) 경합 제어
- `nullable` 프로퍼티는 로컬 변수에 담아야 smart cast 작동
- `validateAndApplyCoupon` — `@Transactional` 없음, `PaymentService` 트랜잭션 내 호출

### point
- 최소 1,000pt / 최대 결제금액 50%, 구매 적립 1% / 리뷰 적립 300pt 고정
- `UserPoint` 없으면 `getOrCreateUserPoint()`로 자동 생성

### review
- `OrderItem` 단위 (주문 1건 N개 상품 → 각각 리뷰 가능)
- `rating`: 0.5 단위만 허용, `(rating * 2) % 1 == 0.0` 검증
- 작성 완료 시 +300pt 자동 적립 (`PointService.earnReviewPoints`)

### like
- 다수 상품 좋아요 상태 → `getLikeStatus(productIds)` 배치 조회 (N+1 방지)

### settlement
- 매일 00:05 전날 자동 정산 (`SettlementScheduler`)
- 상태: `PENDING → CONFIRMED → PAID` 단방향, 순매출은 `totalNetAmount`
