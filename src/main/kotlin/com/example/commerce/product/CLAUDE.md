# product 도메인

상품 등록, 조회, 수정, 삭제 및 재고 관리를 담당한다.

## 폴더 구조
```
product/
├── controller/   # ProductController
├── service/      # ProductService
├── repository/   # ProductRepository
├── entity/       # Product
└── dto/          # ProductCreateRequest, ProductUpdateRequest, ProductResponse
```

## Entity
- `Product` : name, description, price(Long), stock(Int), sellerId(Long)
- 가격 필드는 반드시 `Long` (원 단위 정수)
- BaseEntity 상속 필수

## 주요 ErrorCode (ErrorCode.kt에 추가)
- `PRODUCT_NOT_FOUND` — 존재하지 않는 상품
- `OUT_OF_STOCK` — 재고 부족
- `UNAUTHORIZED_PRODUCT_ACCESS` — 상품 수정/삭제 권한 없음

## API 엔드포인트
| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST   | /api/products | 필요(ADMIN) | 상품 등록 |
| GET    | /api/products | 불필요 | 상품 목록 조회 (페이징) |
| GET    | /api/products/{id} | 불필요 | 상품 단건 조회 |
| PUT    | /api/products/{id} | 필요(ADMIN) | 상품 수정 |
| DELETE | /api/products/{id} | 필요(ADMIN) | 상품 삭제 |

## 재고 관리
- 주문 생성 시 재고 차감, 주문 취소 시 재고 복구
- 재고 변경은 Service 레이어에서 처리, 동시성 이슈 고려 필요
