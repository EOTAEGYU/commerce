# 리뷰 도메인 (Review)

> **구현 상태**: 구현 완료

## 개요

구매 확정(DELIVERED) 상태의 주문에 대해 사용자가 별점 + 텍스트 리뷰를 작성할 수 있는 기능.
리뷰는 **OrderItem 단위**로 연결되어 한 OrderItem에 리뷰 1개만 작성 가능하다.

## 비즈니스 규칙

- 리뷰 작성은 인증된 사용자만 가능하다.
- 반드시 본인 주문의 OrderItem에만 리뷰를 작성할 수 있다.
- 주문 상태가 **DELIVERED**인 경우에만 리뷰 작성 가능하다.
- 한 OrderItem에 리뷰는 **1개**만 작성 가능하다 (DB unique 제약).
- 별점은 **0.5 ~ 5.0** 범위, **0.5 단위**만 허용한다 (1.3, 2.7 등 불가).
- 리뷰 본문은 최대 **500자**다.
- 리뷰 수정/삭제는 **본인**만 가능하다.
- 상품 목록/상세 조회 시 `averageRating`, `reviewCount` 통계가 함께 반환된다.

## 설계 결정

### 리뷰 단위: OrderItem
```
같은 상품 2회 주문 → OrderItem 2개 → 리뷰 2개 작성 가능
한 번에 수량 2개 → OrderItem 1개 → 리뷰 1개만 작성 가능
```

### 별점 타입: Double
```
DB: DOUBLE PRECISION
검증: (rating * 2) % 1 == 0 → 0.5 단위 확인
@DecimalMin("0.5") @DecimalMax("5.0") 으로 범위 제한
```

### N+1 방지 (상품 목록 통계)
```
상품 목록 조회 시 productId 배치 조회:
SELECT r.productId, AVG(r.rating), COUNT(r)
FROM Review r WHERE r.productId IN :productIds
GROUP BY r.productId
```

## 주요 유스케이스

### 1. 리뷰 작성 (createReview)
1. `orderItemId`로 OrderItem 조회 → 없으면 `ORDER_ITEM_NOT_FOUND`
2. Order 소유자 확인 → 다른 회원이면 `ORDER_NOT_OWNED`
3. Order 상태 확인 → DELIVERED 아니면 `ORDER_NOT_DELIVERED`
4. 중복 리뷰 확인 → 이미 존재하면 `REVIEW_ALREADY_EXISTS`
5. 별점 0.5 단위 검증 → 위반 시 `INVALID_RATING`
6. Review 저장 후 ReviewResponse 반환
7. **포인트 적립**: `PointService.earnReviewPoints(userId, reviewId)` 호출 → +300P

### 2. 리뷰 수정 (updateReview)
1. 리뷰 조회 → 없으면 `REVIEW_NOT_FOUND`
2. 본인 확인 → 다르면 `REVIEW_NOT_OWNED`
3. 별점 0.5 단위 검증
4. rating, content 업데이트 (JPA dirty checking)

### 3. 리뷰 삭제 (deleteReview)
1. 리뷰 조회 → 없으면 `REVIEW_NOT_FOUND`
2. 본인 확인 → 다르면 `REVIEW_NOT_OWNED`
3. 삭제

### 4. 상품별 리뷰 목록 (getProductReviews)
- `productId`로 리뷰 목록 조회 (최신순, 페이징)
- 비인증 공개 API

### 5. 내 리뷰 목록 (getMyReviews)
- 로그인 회원의 전체 리뷰 목록 (최신순)

## 파일 구조

```
review/
├── entity/Review.kt                 # JPA 엔티티, BaseEntity 상속
├── repository/ReviewRepository.kt   # 배치 통계 쿼리 포함
├── service/ReviewService.kt         # 비즈니스 로직, 검증 흐름
├── controller/ReviewController.kt   # REST 엔드포인트 5개
├── dto/
│   ├── ReviewRequest.kt             # ReviewCreateRequest, ReviewUpdateRequest
│   └── ReviewResponse.kt            # companion object from() 팩토리 메서드
└── CLAUDE.md                        # 도메인 특화 가이드
```

## API 엔드포인트

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/reviews` | 필요 | 리뷰 작성 |
| PUT | `/api/reviews/{id}` | 필요 (본인) | 리뷰 수정 |
| DELETE | `/api/reviews/{id}` | 필요 (본인) | 리뷰 삭제 |
| GET | `/api/products/{productId}/reviews` | 불필요 | 상품별 리뷰 목록 (페이징) |
| GET | `/api/reviews/my` | 필요 | 내 리뷰 목록 |

## 향후 고려 사항

- **리뷰 작성 가능 여부 프론트 표시**: OrderResponse에 `reviewedOrderItemIds: List<Long>` 추가 여부
- **별점 통계 캐싱**: 상품 수 증가 시 Redis 캐시 적용 고려
- **리뷰 이미지 첨부**: 이미지 업로드 인프라 구축 후 적용
