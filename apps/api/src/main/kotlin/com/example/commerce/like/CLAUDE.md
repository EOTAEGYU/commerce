# like 도메인

상품에 대한 좋아요(찜) 기능을 담당하는 도메인.
한 사용자는 동일 상품에 하나의 좋아요만 가질 수 있다 (UniqueConstraint on user_id + product_id).

## 폴더 구조
```
like/
├── controller/   # ProductLikeController
├── service/      # ProductLikeService
├── repository/   # ProductLikeRepository
├── entity/       # ProductLike
└── dto/          # LikeResponse
```

## Entity

### ProductLike
- `userId(Long)`, `productId(Long)` — 모두 val
- BaseEntity 상속
- (user_id, product_id) unique 제약, 각 컬럼 인덱스 존재

## 비즈니스 규칙

1. toggle: 좋아요가 존재하면 삭제(unlike), 없으면 생성(like) — 응답에 현재 상태와 집계 수 포함
2. 상품 존재 여부 검증: `PRODUCT_NOT_FOUND` throw
3. getMyLikedProducts: 좋아요 목록이 비어있으면 `Page.empty()` 즉시 반환
4. getLikeStatus: `productIds` 배치 조회 후 Map으로 반환 (N+1 방지)

## API 엔드포인트

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | /api/likes/{productId} | 필요 | 좋아요 토글 (좋아요/취소) |
| GET | /api/likes/my | 필요 | 내가 좋아요한 상품 목록 (페이징, size=20) |
| GET | /api/likes/status?productIds=1,2,3 | 필요 | 상품별 좋아요 여부 Map 반환 |

## ErrorCode

- `PRODUCT_NOT_FOUND` — product 도메인 공유 사용

## product/repository/ProductRepository.kt 수정 내역

- `findByIdIn(ids: List<Long>, pageable: Pageable): Page<Product>` 추가
  - Spring Data JPA 네이밍 컨벤션으로 자동 쿼리 생성
