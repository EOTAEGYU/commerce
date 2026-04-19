# 상품 목록 조회 N+1 쿼리 문제

## 문제 상황

`GET /api/products` 상품 목록 API에서 상품 수에 비례해 쿼리가 폭증하는 N+1 문제 발생.

페이지당 20개 상품을 조회할 때 실제 실행 쿼리:

```
1번  SELECT * FROM products WHERE ...       ← 상품 목록 1회
2번  SELECT AVG(rating) FROM reviews WHERE product_id = 1   ← 상품마다
3번  SELECT COUNT(*) FROM reviews WHERE product_id = 1      ← 상품마다
4번  SELECT AVG(rating) FROM reviews WHERE product_id = 2
5번  SELECT COUNT(*) FROM reviews WHERE product_id = 2
...
41번 SELECT AVG(rating) FROM reviews WHERE product_id = 20
42번 SELECT COUNT(*) FROM reviews WHERE product_id = 20
```

**총 쿼리 수: 1 + (N × 2)** — 20개 상품이면 41번, 100개면 201번

## 원인 분석

`ProductService.getList()`에서 상품 목록을 가져온 후, 루프 안에서 각 상품마다 리뷰 통계를 개별 조회한 것이 원인.

```kotlin
// 문제 코드 (의사 코드 — 실제 구현 전 단계)
fun getList(...): Page<ProductResponse> {
    val productPage = productRepository.search(...)
    return productPage.map { product ->
        val avg = reviewRepository.findAverageRatingByProductId(product.id)  // ← N회
        val cnt = reviewRepository.countByProductId(product.id)              // ← N회
        ProductResponse.from(product, avg, cnt)
    }
}
```

`findAverageRatingByProductId`, `countByProductId`는 단건 조회 메서드이므로, 루프 안에서 호출하면 상품 수만큼 쿼리가 발생한다.

단건 조회(`GET /api/products/{id}`)는 상품 1개에 대한 통계만 조회하므로 문제 없음.

## 해결 과정

### 1. 배치 조회 쿼리 추가 (`ReviewRepository`)

상품 ID 목록 전체를 IN 절로 한 번에 조회하는 JPQL 추가:

```kotlin
// ReviewRepository.kt
@Query("SELECT r.productId, AVG(r.rating), COUNT(r) FROM Review r WHERE r.productId IN :productIds GROUP BY r.productId")
fun findStatsByProductIds(productIds: List<Long>): List<Array<Any>>
```

반환 타입을 `List<Array<Any>>`로 설정해 Projection 없이 처리. 각 row는 `[productId, avgRating, reviewCount]`.

### 2. `ProductService.getList()` 수정

루프 전에 배치 조회 → Map 변환 → 루프에서 Map 룩업으로 변경:

```kotlin
fun getList(categoryId: Long?, keyword: String?, pageable: Pageable): Page<ProductResponse> {
    val productPage = productRepository.search(categoryId, pattern, pageable)
    val productIds = productPage.content.map { it.id }

    // 1번 쿼리로 전체 통계 조회
    val statsMap = reviewRepository.findStatsByProductIds(productIds)
        .associate { row ->
            val pid = (row[0] as Number).toLong()
            val avg = (row[1] as Number).toDouble()
            val cnt = (row[2] as Number).toLong()
            pid to Pair(avg, cnt)
        }

    // 루프에서 Map 룩업 (쿼리 없음)
    return productPage.map { product ->
        val stats = statsMap[product.id]
        ProductResponse.from(product, stats?.first, stats?.second ?: 0L)
    }
}
```

### 3. 유사 패턴: 좋아요 상태 배치 조회 (`ProductLikeRepository`)

`GET /api/likes/status?productIds=1,2,3` 에서도 동일한 패턴 적용:

```kotlin
// ProductLikeRepository.kt
@Query("SELECT pl.productId FROM ProductLike pl WHERE pl.userId = :userId AND pl.productId IN :productIds")
fun findLikedProductIds(userId: Long, productIds: List<Long>): List<Long>
```

서비스에서 Set으로 변환 후 룩업:

```kotlin
val likedSet = productLikeRepository.findLikedProductIds(userId, productIds).toSet()
return productIds.associateWith { it in likedSet }
```

## 결과

| 구분 | 수정 전 | 수정 후 |
|------|--------|--------|
| 상품 20개 쿼리 수 | 41번 | 2번 |
| 상품 100개 쿼리 수 | 201번 | 2번 |
| 쿼리 수 증가 방식 | O(N) | O(1) |

## 주의사항

- `productIds`가 빈 리스트일 때 `IN ()` 구문이 DB에 따라 문법 오류를 낼 수 있다.  
  → `productPage.content`가 비어 있으면 `findStatsByProductIds`를 호출하지 않도록 early return 처리.
- 단건 조회(`getOne`)는 여전히 `findAverageRatingByProductId` + `countByProductId` 2회 호출.  
  단건은 쿼리 수가 고정(2회)이므로 배치 쿼리 미적용.
- `Array<Any>`의 Number 캐스팅은 DB 드라이버에 따라 타입이 다를 수 있어 `as Number` → `.toLong()` / `.toDouble()` 순서로 안전하게 변환.

## 참고

- JPA N+1 해결 일반 전략: `@EntityGraph`, `JOIN FETCH`, 배치 IN 쿼리
- 이 프로젝트는 Product ↔ Review가 외래키 없이 `productId(Long)` 참조 방식이라 `JOIN FETCH` 불가 → IN 배치 쿼리 선택
- 관련 파일:
  - `product/service/ProductService.kt` — `getList()` 메서드
  - `review/repository/ReviewRepository.kt` — `findStatsByProductIds()`
  - `like/repository/ProductLikeRepository.kt` — `findLikedProductIds()`
