# cart 도메인

회원의 장바구니 아이템 관리를 담당한다.

## 폴더 구조

```
cart/
├── controller/   # CartController
├── service/      # CartService
├── repository/   # CartRepository, CartItemRepository
├── entity/       # Cart, CartItem
└── dto/          # CartItemAddRequest, CartItemUpdateRequest, CartResponse, CartItemResponse
```

## Entity

### Cart
- `userId(Long)` — UNIQUE, 회원당 1개
- `items: MutableList<CartItem>` — CascadeType.ALL, orphanRemoval=true

### CartItem
- `cart(Cart)` — ManyToOne LAZY
- `productId(Long)`, `productOptionId(Long)` — 참조용
- `quantity(Int)` — var (수량 변경 가능)
- `price(Long)` — 담을 당시 가격 스냅샷 (val)

## 주요 비즈니스 규칙

- 장바구니 없으면 첫 상품 추가 시 자동 생성
- 동일 optionId 항목은 수량 합산
- 재고 확인: `option.stock < quantity` → OUT_OF_STOCK
- 주문 생성 완료 후 `cart.items.clear()` 호출

## 주요 ErrorCode

- `CART_ITEM_NOT_FOUND` — 없는 항목 접근
- `CART_ITEM_NOT_OWNED` — 다른 회원의 항목 접근

## API 엔드포인트

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| GET | /api/cart | 필요 | 장바구니 조회 |
| POST | /api/cart/items | 필요 | 상품 추가 |
| PUT | /api/cart/items/{id} | 필요 | 수량 변경 |
| DELETE | /api/cart/items/{id} | 필요 | 상품 제거 |
