# TODO (2026-04-09)

## 완료
- [x] 공통 모듈 (BaseEntity, ApiResponse, ErrorCode, CustomException, GlobalExceptionHandler)
- [x] 회원 도메인 (가입, 로그인, JWT, Spring Security) + 테스트
- [x] 카테고리 도메인 (2depth 계층 구조, CRUD) + 테스트

---

## 완료 (계속)
- [x] 상품 도메인 (ProductOption 재고, Pessimistic Lock) + 테스트

---

## 다음: 장바구니 도메인

### 1. CartItem Entity
- [ ] `Cart` 엔티티 (userId)
- [ ] `CartItem` 엔티티 (cart, productId, productOptionId, quantity, price)

### 2. Repository
- [ ] `CartRepository`
- [ ] `CartItemRepository`

### 3. CartService
- [ ] 장바구니 조회
- [ ] 상품 추가 (optionId 포함, 재고 확인)
- [ ] 수량 변경
- [ ] 아이템 삭제

### 4. CartController
- [ ] `GET /api/cart` (인증 필요)
- [ ] `POST /api/cart/items` (인증 필요)
- [ ] `PUT /api/cart/items/{id}` (인증 필요)
- [ ] `DELETE /api/cart/items/{id}` (인증 필요)

### 5. 테스트 코드
- [ ] `CartServiceTest`
- [ ] `CartRepositoryTest`
- [ ] `CartControllerTest`

### 6. 마무리
- [ ] `./gradlew test` 전체 통과 확인
- [ ] `./gradlew build` 확인
- [ ] 커밋

---

## 이후 예정
- [ ] 주문
- [ ] 결제
