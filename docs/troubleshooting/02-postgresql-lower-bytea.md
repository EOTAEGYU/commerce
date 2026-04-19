# PostgreSQL `lower(bytea)` 에러 — JPQL null 파라미터 타입 추론 실패

## 문제 상황

`GET /api/products` 요청 시 500 에러 반환, 프론트엔드에 "상품을 불러오는 중 오류가 발생했습니다." 표시.

백엔드 로그:

```
ERROR: function lower(bytea) does not exist
  Hint: No function matches the given name and argument types.
        You might need to add explicit type casts.
  Position: 220

Resolved [org.springframework.dao.InvalidDataAccessResourceUsageException:
JDBC exception executing SQL [...lower(p1_0.name) like lower(('%'||?||'%'))...]]
```

## 원인 분석

상품 키워드 검색 구현 시 작성한 JPQL:

```kotlin
@Query("""
    SELECT p FROM Product p
    WHERE (:categoryId IS NULL OR p.categoryId = :categoryId)
      AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
""")
fun search(categoryId: Long?, keyword: String?, pageable: Pageable): Page<Product>
```

keyword가 `null`일 때 Hibernate가 생성하는 SQL:

```sql
WHERE (? is null OR p.category_id = ?)
  AND (? is null OR lower(p.name) like lower(('%' || ? || '%')))
```

**핵심 원인**: PostgreSQL이 `null` 파라미터(`?`)의 타입을 추론할 때, 문자열 컨텍스트임에도 불구하고 `bytea`로 추론함. `lower(bytea)` 함수는 존재하지 않으므로 쿼리 실행 실패.

- `Long?`(숫자)는 PostgreSQL이 numeric으로 정확히 추론 → 문제없음
- `String?`(문자열)의 null은 PostgreSQL이 `bytea`로 추론 → `lower(bytea)` 에러 발생

## 해결 과정

null 파라미터를 넘기지 않고, keyword 유무에 따라 패턴을 미리 가공해서 항상 문자열을 넘기도록 변경.

### Repository — null 파라미터 제거

```kotlin
// 변경 전
@Query("""
    SELECT p FROM Product p
    WHERE (:categoryId IS NULL OR p.categoryId = :categoryId)
      AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
""")
fun search(categoryId: Long?, keyword: String?, pageable: Pageable): Page<Product>

// 변경 후
@Query("""
    SELECT p FROM Product p
    WHERE (:categoryId IS NULL OR p.categoryId = :categoryId)
      AND LOWER(p.name) LIKE :pattern
""")
fun search(categoryId: Long?, pattern: String, pageable: Pageable): Page<Product>
```

### Service — 패턴 가공 후 전달

```kotlin
// 변경 전
fun getList(categoryId: Long?, keyword: String?, pageable: Pageable): Page<ProductResponse> =
    productRepository.search(categoryId, keyword?.takeIf { it.isNotBlank() }, pageable)
        .map { ProductResponse.from(it) }

// 변경 후
fun getList(categoryId: Long?, keyword: String?, pageable: Pageable): Page<ProductResponse> {
    val pattern = keyword?.takeIf { it.isNotBlank() }?.let { "%${it.lowercase()}%" } ?: "%"
    return productRepository.search(categoryId, pattern, pageable).map { ProductResponse.from(it) }
}
```

- keyword 없음 → `%` (전체 매칭, PostgreSQL이 문자열로 인식)
- keyword 있음 → `%keyword%` (부분 일치, 소문자 변환)

## 결과

`GET /api/products` 200 정상 응답. 키워드 검색(`?keyword=나이키`) 및 카테고리 필터(`?categoryId=1`)도 정상 동작.

## 교훈

- PostgreSQL + Hibernate에서 JPQL의 `null` 문자열 파라미터는 `bytea`로 추론될 수 있음
- **null을 조건 분기에 활용하는 패턴은 문자열 타입에 위험** → sentinel 값(`%`) 또는 명시적 타입 캐스트(`CAST(:keyword AS string)`) 사용
- `Long?`(숫자형)는 해당 문제 없음
