# 엔드포인트 목록

> 상세 스펙(요청/응답 스키마)은 Swagger UI에서 확인: http://localhost:8080/swagger-ui.html

## 회원 (User)

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/users/signup` | 불필요 | 회원가입 |
| POST | `/api/users/signin` | 불필요 | 로그인 → JWT 반환 |
| GET | `/api/users/me` | **필요** | 내 프로필 조회 |

### POST /api/users/signup
```json
// Request
{
  "email": "user@example.com",    // 이메일 형식, 필수
  "password": "password123",      // 8자 이상, 필수
  "name": "홍길동"                 // 필수
}

// Response 201
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER"
  },
  "error": null
}
```

### POST /api/users/signin
```json
// Request
{
  "email": "user@example.com",
  "password": "password123"
}

// Response 200
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer"
  },
  "error": null
}
```

### GET /api/users/me
```
// Request Header
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

// Response 200
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER"
  },
  "error": null
}
```

---

## 상품 (Product) — 구현 예정

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/products` | 불필요 | 상품 목록 조회 |
| GET | `/api/products/{id}` | 불필요 | 상품 상세 조회 |
| POST | `/api/products` | **필요** (ADMIN) | 상품 등록 |
| PUT | `/api/products/{id}` | **필요** (ADMIN) | 상품 수정 |
| DELETE | `/api/products/{id}` | **필요** (ADMIN) | 상품 삭제 |

## 장바구니 (Cart) — 구현 예정

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/cart` | **필요** | 장바구니 조회 |
| POST | `/api/cart/items` | **필요** | 상품 추가 |
| PUT | `/api/cart/items/{id}` | **필요** | 수량 변경 |
| DELETE | `/api/cart/items/{id}` | **필요** | 상품 제거 |

## 주문 (Order) — 구현 예정

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/orders` | **필요** | 주문 생성 |
| GET | `/api/orders` | **필요** | 내 주문 목록 |
| GET | `/api/orders/{id}` | **필요** | 주문 상세 |
| POST | `/api/orders/{id}/cancel` | **필요** | 주문 취소 |

## 결제 (Payment) — 구현 예정

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/payments` | **필요** | 결제 요청 |
| GET | `/api/payments/{orderId}` | **필요** | 결제 상태 조회 |
