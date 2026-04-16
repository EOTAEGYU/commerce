# review 도메인

구매 완료(DELIVERED) 주문의 OrderItem 단위로 리뷰를 작성하는 도메인.
한 OrderItem당 리뷰는 1개만 허용 (UniqueConstraint on order_item_id).

## 폴더 구조
```
review/
├── controller/   # ReviewController
├── service/      # ReviewService
├── repository/   # ReviewRepository
├── entity/       # Review
└── dto/          # ReviewCreateRequest, ReviewUpdateRequest, ReviewResponse
```

## Entity

### Review
- `userId(Long)`, `productId(Long)`, `orderId(Long)`, `orderItemId(Long)` — 모두 val
- `rating(Double)` — var (수정 가능), 0.5 단위, 0.5 ~ 5.0
- `content(String)` — var (수정 가능), 최대 500자
- BaseEntity 상속, order_item_id unique 제약

## 비즈니스 규칙

1. 리뷰 작성 가능 조건: order.status == DELIVERED + 본인 주문 + 해당 orderItem에 리뷰 미존재
2. rating 유효성: (rating * 2) % 1 == 0.0 (0.5 단위 검증)
3. 수정/삭제: 본인 리뷰만 가능

## API 엔드포인트

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | /api/reviews | 필요 | 리뷰 작성 |
| PUT | /api/reviews/{id} | 필요 (본인) | 리뷰 수정 |
| DELETE | /api/reviews/{id} | 필요 (본인) | 리뷰 삭제 |
| GET | /api/products/{productId}/reviews | 불필요 | 상품별 리뷰 목록 (페이징) |
| GET | /api/reviews/my | 필요 | 내 리뷰 목록 |

## ErrorCode

- `REVIEW_NOT_FOUND` — 존재하지 않는 리뷰
- `REVIEW_ALREADY_EXISTS` — 이미 리뷰를 작성한 OrderItem
- `REVIEW_NOT_OWNED` — 타인의 리뷰 수정/삭제 시도
- `ORDER_NOT_DELIVERED` — DELIVERED 미만 상태의 주문에 리뷰 작성 시도
- `INVALID_RATING` — 0.5 단위가 아닌 rating 값
- `ORDER_ITEM_NOT_FOUND` — 존재하지 않는 OrderItem

## ProductService 통계 병합

- 단건 조회(`getOne`): `findAverageRatingByProductId`, `countByProductId` 각각 호출
- 목록 조회(`getList`): `findStatsByProductIds` 배치 조회 후 Map으로 변환하여 병합 (N+1 방지)
- `ProductResponse`에 `averageRating: Double?`, `reviewCount: Long` 필드 추가 (기본값으로 하위 호환 유지)
