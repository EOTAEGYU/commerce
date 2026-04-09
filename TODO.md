# TODO (2026-04-09)

## 완료
- [x] 공통 모듈 (BaseEntity, ApiResponse, ErrorCode, CustomException, GlobalExceptionHandler)
- [x] 회원 도메인 (가입, 로그인, JWT, Spring Security)
- [x] 카테고리 도메인 (2depth 계층 구조, CRUD)

---

## 다음: 상품 도메인

### 1. ErrorCode 추가
- [ ] `PRODUCT_NOT_FOUND` (404)
- [ ] `PRODUCT_OPTION_NOT_FOUND` (404)
- [ ] `OUT_OF_STOCK` (409)

### 2. Product Entity
- [ ] `Product` 엔티티 (name, description, price: Long, categoryId: Long)
- [ ] `ProductOption` 엔티티 (product, size, color, stock: Int)

### 3. Repository
- [ ] `ProductRepository`
- [ ] `ProductOptionRepository` (`@Lock(PESSIMISTIC_WRITE)` findByIdWithLock 포함)

### 4. ProductService
- [ ] 상품 등록 (옵션 포함)
- [ ] 상품 목록 조회 (페이징, 카테고리 필터)
- [ ] 상품 단건 조회 (옵션 목록 포함)
- [ ] 상품 수정
- [ ] 상품 삭제
- [ ] `decreaseStock` (Pessimistic Lock)
- [ ] `increaseStock` (주문 취소용)

### 5. ProductController
- [ ] `POST /api/products` (ADMIN)
- [ ] `GET /api/products` (공개)
- [ ] `GET /api/products/{id}` (공개)
- [ ] `PUT /api/products/{id}` (ADMIN)
- [ ] `DELETE /api/products/{id}` (ADMIN)

### 6. SecurityConfig 업데이트
- [ ] GET `/api/products/**` → 인증 불필요

### 7. 마무리
- [ ] `./gradlew build` 확인
- [ ] 커밋

---

## 이후 예정
- [ ] 장바구니 (optionId 포함)
- [ ] 주문
- [ ] 결제
