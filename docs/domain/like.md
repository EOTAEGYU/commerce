# 좋아요 도메인 (Like)

> **구현 상태**: 구현 완료

## 개요

사용자가 마음에 드는 상품에 좋아요(찜)를 표시하고, 좋아요한 상품 목록을 확인할 수 있는 기능.
토글 방식으로 동작하여 한 번 누르면 추가, 다시 누르면 취소된다.

## 비즈니스 규칙

- 좋아요는 인증된 사용자만 가능하다.
- 한 사용자는 동일 상품에 **1개**의 좋아요만 가질 수 있다 (DB unique 제약).
- 좋아요 API는 **토글 방식** — 좋아요 존재 시 취소, 없으면 추가.
- 응답에는 현재 좋아요 상태(`liked`)와 집계 수(`likeCount`)가 포함된다.
- 존재하지 않는 상품에 좋아요 시 `PRODUCT_NOT_FOUND` 예외.

## 설계 결정

### 토글 API 단일 엔드포인트
```
POST /api/likes/{productId}  →  liked: true (추가) 또는 liked: false (취소)
```
추가/취소를 별도 엔드포인트로 나누지 않고 토글로 통일. UI에서 현재 상태만 확인하면 됨.

### ProductResponse에 isLiked/likeCount 미포함
`isLiked`는 요청 유저에 의존하는 값이라 SSR 캐시와 궁합이 나쁘다.
별도 `/api/likes/status` API로 분리하여 클라이언트에서 batch 조회.

### 배치 상태 조회 (N+1 방지)
```
GET /api/likes/status?productIds=1,2,3
→ findLikedProductIds() 단일 JPQL 쿼리
→ productIds.associateWith { it in likedSet } 로 Map 구성
```

## 주요 유스케이스

### 1. 좋아요 토글 (toggle)
1. `productId`로 상품 존재 확인 → 없으면 `PRODUCT_NOT_FOUND`
2. `(userId, productId)` 조회 → 존재하면 delete, 없으면 save
3. `countByProductId()` 재집계 → `LikeResponse` 반환

### 2. 내 좋아요 목록 (getMyLikedProducts)
1. `findAllByUserId()`로 좋아요한 productId 목록 추출
2. 비어있으면 `Page.empty()` 즉시 반환 (DB 조회 없음)
3. `productRepository.findByIdIn(ids, pageable)`으로 상품 페이징 조회

### 3. 좋아요 상태 배치 조회 (getLikeStatus)
1. `findLikedProductIds(userId, productIds)` 단일 쿼리
2. `Map<Long, Boolean>` 구성 후 반환

## 파일 구조

```
like/
├── entity/ProductLike.kt              # JPA 엔티티, unique 제약 + 인덱스
├── repository/ProductLikeRepository.kt # 배치 쿼리 포함
├── service/ProductLikeService.kt      # 비즈니스 로직
├── controller/ProductLikeController.kt # REST 엔드포인트 3개
├── dto/LikeResponse.kt                # 토글 응답 DTO
└── CLAUDE.md                          # 도메인 특화 가이드
```

## API 엔드포인트

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/likes/{productId}` | 필요 | 좋아요 토글 |
| GET | `/api/likes/my` | 필요 | 내 좋아요 상품 목록 (페이징) |
| GET | `/api/likes/status?productIds=1,2,3` | 필요 | 복수 상품 좋아요 여부 |

## 프론트엔드 연동

| 컴포넌트 | 역할 |
|----------|------|
| `LikeButton.tsx` | 하트 토글 버튼. 낙관적 업데이트, 비로그인 시 `/signin` 리다이렉트 |
| `ProductLikeSection.tsx` | 상세 페이지용 CSR 래퍼. `status` API로 초기 좋아요 상태 fetch |
| `ProductGridWithLikes.tsx` | 상품 목록 CSR 래퍼. `status` API 배치 조회 후 각 ProductCard에 전달 |
| `app/likes/page.tsx` | 찜목록 페이지. `my` API로 좋아요한 상품 목록 표시 |

## 향후 고려 사항

- **좋아요 수 캐싱**: 인기 상품의 경우 Redis로 likeCount 캐싱 고려
- **상품 목록에 likeCount 표시**: ProductResponse에 통계 포함 여부 (현재 별도 API)
- **알림 기능**: 찜한 상품 가격 변동 시 알림 전송
