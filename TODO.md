# TODO (2026-04-09)

## 완료
- [x] 공통 모듈 (BaseEntity, ApiResponse, ErrorCode, CustomException, GlobalExceptionHandler)
- [x] 회원 도메인 (가입, 로그인, JWT, Spring Security) + 테스트
- [x] 카테고리 도메인 (2depth 계층 구조, CRUD) + 테스트

---

## 완료 (계속)
- [x] 상품 도메인 (ProductOption 재고, Pessimistic Lock) + 테스트

---

## 완료 (계속)
- [x] 장바구니 도메인 (Cart/CartItem, CRUD, 재고 확인) + 테스트

---

## 완료 (계속)
- [x] 주문 도메인 (Order/OrderItem, CRUD, 재고 차감/복원, 스냅샷) + 테스트

---

## 이후 예정: 결제 도메인

### 1. Entity
- [ ] `PaymentMethod` enum (CARD, BANK_TRANSFER 등)
- [ ] `PaymentStatus` enum (REQUESTED, COMPLETED, FAILED, REFUNDED)
- [ ] `Payment` 엔티티 (orderId, userId, amount, method, status, pgTransactionId)

### 2. PaymentService
- [ ] 결제 요청 (PENDING 주문만 가능, PG 연동 Mock)
- [ ] 결제 성공 처리 → Order 상태 PAID 변경
- [ ] 결제 실패 처리 → 재고 복원 + Order 상태 CANCELLED 변경

### 3. PaymentController
- [ ] `POST /api/payments` (인증 필요)

### 4. 테스트 코드
- [ ] `PaymentServiceTest`
- [ ] `PaymentControllerTest`

### 5. 마무리
- [ ] `./gradlew test` 전체 통과 확인
- [ ] `./gradlew build` 확인
- [ ] 커밋
