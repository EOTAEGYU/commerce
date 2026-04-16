# TODO — 리뷰 도메인 구현 (2026-04-16)

## 구현 체크리스트

### Step 1. Review 도메인 생성
- [x] `review/entity/Review.kt` — BaseEntity 상속, unique FK on order_item_id
- [x] `review/repository/ReviewRepository.kt` — 배치 통계 쿼리 포함
- [x] `review/dto/ReviewRequest.kt` — ReviewCreateRequest, ReviewUpdateRequest
- [x] `review/dto/ReviewResponse.kt` — companion object from() 팩토리 메서드
- [x] `review/service/ReviewService.kt` — 검증 흐름 (OrderItem 조회 → 소유자 → DELIVERED → 중복 → 별점 단위)
- [x] `review/controller/ReviewController.kt` — 5개 엔드포인트

### Step 2. ErrorCode 추가
- [x] `common/ErrorCode.kt` — REVIEW_NOT_FOUND, REVIEW_ALREADY_EXISTS, REVIEW_NOT_OWNED, ORDER_NOT_DELIVERED, INVALID_RATING
- [x] `common/ErrorCode.kt` — ORDER_ITEM_NOT_FOUND 추가

### Step 3. ProductResponse 통계 필드 추가
- [x] `product/dto/ProductResponse.kt` — averageRating: Double?, reviewCount: Long 추가
- [x] `product/service/ProductService.kt` — ReviewRepository 주입, 단건/목록 통계 병합

### Step 4. SecurityConfig 경로 허용
- [x] `common/security/SecurityConfig.kt` — GET /api/products/** 이미 허용됨 (수정 불필요)

### Step 5. 테스트 작성
- [x] `review/service/ReviewServiceTest.kt` — MockK, 검증 실패 케이스 + 정상 케이스 (18케이스)
- [x] `review/controller/ReviewControllerTest.kt` — @WebMvcTest, 상태코드 검증 (17케이스)

### Step 6. 빌드 및 검증
- [x] `./gradlew build` 빌드 성공 확인
- [x] `./gradlew test` 전체 테스트 통과 확인 (228 tests)

### Step 7. 커밋 및 문서 최신화
- [x] `feat(review): 리뷰 도메인 구현` 커밋
- [x] `docs/domain/review.md` 신규 작성
- [x] `docs/api/endpoints.md` 리뷰 엔드포인트 추가
- [x] `docs/database/schema.md` reviews 테이블 + 관계도 업데이트
- [x] `docs/development/error-codes.md` 리뷰 에러코드 추가

---

## API 엔드포인트

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | /api/reviews | 인증 | 리뷰 작성 |
| PUT | /api/reviews/{id} | 인증(본인) | 리뷰 수정 |
| DELETE | /api/reviews/{id} | 인증(본인) | 리뷰 삭제 |
| GET | /api/products/{productId}/reviews | 공개 | 상품별 리뷰 목록(페이징) |
| GET | /api/reviews/my | 인증 | 내 리뷰 목록 |

---

## 완료 이력 (이전 작업)

- [x] 공통 모듈, 회원, 카테고리, 상품, 장바구니, 주문, 결제 도메인
- [x] 멀티모듈 구조, CORS, 관리자 초기화, 상품 이미지 URL
- [x] 프론트엔드 전체 (Next.js 16 + 무신사 UI 리디자인)
- [x] 백엔드 보완 (키워드 검색, 프로필 수정, JWT env 처리)
