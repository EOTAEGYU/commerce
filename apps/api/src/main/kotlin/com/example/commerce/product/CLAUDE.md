# product 도메인

의류/신발 패션 자사몰의 상품 등록, 조회, 수정, 삭제 및 옵션별 재고 관리를 담당한다.

## 폴더 구조
```
product/
├── controller/   # ProductController
├── service/      # ProductService
├── repository/   # ProductRepository, ProductOptionRepository
├── entity/       # Product, ProductOption
└── dto/          # ProductCreateRequest, ProductUpdateRequest, ProductResponse, ProductOptionResponse
```

## Entity

### Product
- `name`, `description`, `price(Long)`, `categoryId(Long)`
- `stock` 필드 없음 — 재고는 ProductOption에서 관리
- BaseEntity 상속 필수
- 가격 필드는 반드시 `Long` (원 단위 정수)

### ProductOption
- `product(Product)`, `size(String)`, `color(String)`, `stock(Int)`
- 재고는 옵션(사이즈+컬러 조합) 단위로 관리
- BaseEntity 상속 필수

## 주요 ErrorCode (ErrorCode.kt에 추가)
- `PRODUCT_NOT_FOUND` — 존재하지 않는 상품
- `PRODUCT_OPTION_NOT_FOUND` — 존재하지 않는 상품 옵션
- `OUT_OF_STOCK` — 재고 부족

## API 엔드포인트
| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST   | /api/products | 필요(ADMIN) | 상품 등록 (옵션 포함) |
| GET    | /api/products | 불필요 | 상품 목록 조회 (페이징, categoryId 필터, keyword 검색) |
| GET    | /api/products/{id} | 불필요 | 상품 단건 조회 (옵션 목록 포함) |
| PUT    | /api/products/{id} | 필요(ADMIN) | 상품 수정 |
| DELETE | /api/products/{id} | 필요(ADMIN) | 상품 삭제 |

## 재고 관리 (Pessimistic Lock)
- 재고는 ProductOption 단위로 관리
- 주문 생성 시 해당 옵션 재고 차감, 주문 취소 시 복구
- 동시성 제어: `@Lock(LockModeType.PESSIMISTIC_WRITE)` 사용
- 재고 변경은 Service 레이어에서만 처리

```kotlin
// ProductOptionRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM ProductOption o WHERE o.id = :id")
fun findByIdWithLock(id: Long): ProductOption?
```
