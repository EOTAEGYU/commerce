# 엔드포인트 목록

> 상세 스펙(요청/응답 스키마)은 Swagger UI에서 확인: http://localhost:8080/swagger-ui.html

## 회원 (User) — 구현 완료

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/users/signup` | 불필요 | 회원가입 |
| POST | `/api/users/signin` | 불필요 | 로그인 → JWT 반환 |
| GET | `/api/users/me` | **필요** | 내 프로필 조회 |

### POST /api/users/signup
```json
// Request
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}

// Response 201
{
  "success": true,
  "data": { "id": 1, "email": "user@example.com", "name": "홍길동", "role": "USER" },
  "error": null
}
```

### POST /api/users/signin
```json
// Request
{ "email": "user@example.com", "password": "password123" }

// Response 200
{
  "success": true,
  "data": { "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer" },
  "error": null
}
```

---

## 카테고리 (Category) — 구현 완료

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/categories` | **필요** (ADMIN) | 카테고리 등록 |
| GET | `/api/categories` | 불필요 | 전체 카테고리 트리 조회 |
| PUT | `/api/categories/{id}` | **필요** (ADMIN) | 카테고리 수정 |
| DELETE | `/api/categories/{id}` | **필요** (ADMIN) | 카테고리 삭제 |

---

## 상품 (Product) — 구현 완료

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/products` | 불필요 | 상품 목록 조회 (페이징, 카테고리 필터) |
| GET | `/api/products/{id}` | 불필요 | 상품 상세 조회 (옵션 목록 포함) |
| POST | `/api/products` | **필요** (ADMIN) | 상품 등록 (옵션 포함) |
| PUT | `/api/products/{id}` | **필요** (ADMIN) | 상품 수정 |
| DELETE | `/api/products/{id}` | **필요** (ADMIN) | 상품 삭제 |

---

## 장바구니 (Cart) — 구현 완료

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/cart` | **필요** | 장바구니 조회 |
| POST | `/api/cart/items` | **필요** | 상품 추가 (optionId 포함) |
| PUT | `/api/cart/items/{id}` | **필요** | 수량 변경 |
| DELETE | `/api/cart/items/{id}` | **필요** | 상품 제거 |

---

## 주문 (Order) — 구현 완료

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/orders` | **필요** | 주문 생성 (장바구니 기반) |
| GET | `/api/orders` | **필요** | 내 주문 목록 |
| GET | `/api/orders/{id}` | **필요** | 주문 상세 |
| POST | `/api/orders/{id}/cancel` | **필요** | 주문 취소 (PENDING/PAID만 가능) |

---

## 결제 (Payment) — 구현 완료

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/payments` | **필요** | 결제 요청 |
| GET | `/api/payments/{orderId}` | **필요** | 결제 상태 조회 |
