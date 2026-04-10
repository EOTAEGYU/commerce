# TODO (2026-04-11)

## 완료
- [x] 공통 모듈 (BaseEntity, ApiResponse, ErrorCode, CustomException, GlobalExceptionHandler)
- [x] 회원 도메인 (가입, 로그인, JWT, Spring Security) + 테스트
- [x] 카테고리 도메인 (2depth 계층 구조, CRUD) + 테스트
- [x] 상품 도메인 (ProductOption 재고, Pessimistic Lock) + 테스트
- [x] 장바구니 도메인 (Cart/CartItem, CRUD, 재고 확인) + 테스트
- [x] 주문 도메인 (Order/OrderItem, CRUD, 재고 차감/복원, 스냅샷) + 테스트
- [x] 결제 도메인 (Payment, Mock PG, 결제 실패 재고 복원, OrderExpirationScheduler) + 테스트
- [x] 멀티모듈 구조 전환 (src/ → apps/api/src/)
- [x] CORS 설정 추가 (localhost:3000, localhost:5173)

---

## 프론트 연동 전 정비 (백엔드)
- [ ] ADMIN 계정 초기화 방법 마련 (data.sql로 초기 ADMIN 계정 삽입)
- [ ] 상품 이미지 URL 필드 추가 (Product 엔티티 + DTO + 테스트)
- [ ] ddl-auto를 create-drop → update 로 변경
