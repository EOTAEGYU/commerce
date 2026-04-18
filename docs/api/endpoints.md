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

---

## 리뷰 (Review) — 구현 완료

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/reviews` | **필요** | 리뷰 작성 (DELIVERED 주문의 OrderItem 단위) |
| PUT | `/api/reviews/{id}` | **필요** (본인) | 리뷰 수정 |
| DELETE | `/api/reviews/{id}` | **필요** (본인) | 리뷰 삭제 |
| GET | `/api/products/{productId}/reviews` | 불필요 | 상품별 리뷰 목록 (페이징) |
| GET | `/api/reviews/my` | **필요** | 내 리뷰 목록 |

### POST /api/reviews
```json
// Request
{
  "orderItemId": 1,
  "rating": 4.5,
  "content": "사이즈가 딱 맞고 품질이 좋습니다."
}

// Response 200
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "userName": "홍길동",
    "productId": 2,
    "orderItemId": 1,
    "rating": 4.5,
    "content": "사이즈가 딱 맞고 품질이 좋습니다.",
    "createdAt": "2026-04-16T10:00:00",
    "updatedAt": "2026-04-16T10:00:00"
  },
  "error": null
}
```

### GET /api/products/{productId}/reviews

```json
// Response 200 (Page<ReviewResponse>)
{
  "success": true,
  "data": {
    "content": [...],
    "totalElements": 5,
    "totalPages": 1,
    "size": 20,
    "number": 0
  },
  "error": null
}
```

---

## 좋아요 (Like) — 구현 완료

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/likes/{productId}` | **필요** | 좋아요 토글 (추가/취소) |
| GET | `/api/likes/my` | **필요** | 내 좋아요 상품 목록 (페이징) |
| GET | `/api/likes/status?productIds=1,2,3` | **필요** | 복수 상품 좋아요 여부 조회 |

### POST /api/likes/{productId}
```json
// Response 200
{
  "success": true,
  "data": {
    "productId": 1,
    "liked": true,
    "likeCount": 42
  },
  "error": null
}
```

### GET /api/likes/my
```json
// Response 200 (Page<ProductResponse>)
{
  "success": true,
  "data": {
    "content": [...],
    "totalElements": 5,
    "totalPages": 1,
    "size": 20,
    "number": 0
  },
  "error": null
}
```

### GET /api/likes/status?productIds=1,2,3
```json
// Response 200 (Map<Long, Boolean>)
{
  "success": true,
  "data": {
    "1": true,
    "2": false,
    "3": true
  },
  "error": null
}
```

---

## 쿠폰 (Coupon) — 구현 완료

### 사용자 API

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/coupons/{templateId}/issue` | **필요** | 쿠폰 발급 |
| GET | `/api/coupons/me` | **필요** | 내 쿠폰 목록 조회 |

### 관리자 API

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/admin/coupons` | **필요** (ADMIN) | 쿠폰 템플릿 생성 |
| GET | `/api/admin/coupons` | **필요** (ADMIN) | 전체 쿠폰 템플릿 조회 |
| PATCH | `/api/admin/coupons/{id}/toggle` | **필요** (ADMIN) | 쿠폰 활성화/비활성화 토글 |

### POST /api/admin/coupons
```json
// Request
{
  "name": "신규회원 10% 할인",
  "discountType": "RATE",
  "discountValue": 10,
  "maxDiscountAmount": 5000,
  "minOrderAmount": 30000,
  "categoryId": null,
  "totalQuantity": 1000,
  "validFrom": "2026-01-01T00:00:00",
  "validUntil": "2026-12-31T23:59:59"
}

// Response 200
{
  "success": true,
  "data": { "id": 1, "name": "신규회원 10% 할인", "discountType": "RATE", ... },
  "error": null
}
```

### POST /api/payments (쿠폰 적용 시)
```json
// Request (couponId 추가)
{
  "orderId": 1,
  "method": "CARD",
  "couponId": 3,
  "simulateFailure": false
}

// Response 200 (discountAmount, originalAmount 추가)
{
  "success": true,
  "data": {
    "id": 1,
    "orderId": 1,
    "amount": 27000,
    "originalAmount": 30000,
    "discountAmount": 3000,
    "couponId": 3,
    "method": "CARD",
    "status": "COMPLETED"
  },
  "error": null
}
```
