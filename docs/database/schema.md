# 데이터베이스 스키마

## BaseEntity (공통 필드)

모든 엔티티는 `BaseEntity`를 상속하여 다음 필드를 공통으로 가집니다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `created_at` | TIMESTAMP | 생성 시각 (자동, 수정 불가) |
| `updated_at` | TIMESTAMP | 최종 수정 시각 (자동) |

---

## users

회원 정보 테이블.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 회원 ID |
| `email` | VARCHAR | NOT NULL, UNIQUE | 이메일 (로그인 ID) |
| `password` | VARCHAR | NOT NULL | BCrypt 해시 비밀번호 |
| `name` | VARCHAR | NOT NULL | 회원 이름 |
| `role` | VARCHAR | NOT NULL | 권한 (`USER`, `ADMIN`) |
| `created_at` | TIMESTAMP | NOT NULL | 가입 시각 |
| `updated_at` | TIMESTAMP | NOT NULL | 최종 수정 시각 |

---

## categories (구현 예정)

상품 카테고리 테이블. 2depth 자기 참조 구조.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 카테고리 ID |
| `name` | VARCHAR | NOT NULL | 카테고리명 |
| `parent_id` | BIGINT | FK(categories), NULL 허용 | 부모 카테고리 (null이면 대분류) |
| `display_order` | INT | NOT NULL, DEFAULT 0 | 정렬 순서 |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

---

## products (구현 예정)

상품 테이블. 재고는 product_options에서 관리.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 상품 ID |
| `name` | VARCHAR | NOT NULL | 상품명 |
| `description` | TEXT | | 상품 설명 |
| `price` | BIGINT | NOT NULL | 가격 (원 단위) |
| `category_id` | BIGINT | NOT NULL, FK(categories) | 카테고리 ID |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

---

## product_options (구현 예정)

상품 옵션(사이즈+컬러 조합) 및 재고 테이블.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 옵션 ID |
| `product_id` | BIGINT | NOT NULL, FK(products) | 상품 ID |
| `size` | VARCHAR | NOT NULL | 사이즈 (S/M/L/XL 등) |
| `color` | VARCHAR | NOT NULL | 컬러 (블랙/화이트 등) |
| `stock` | INT | NOT NULL | 재고 수량 |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

---

## 엔티티 관계도 (현재 → 예정)

```
categories
  │
  └──< products (category_id)        N:1 — 상품은 하나의 소분류에 속함
          │
          └──< product_options        1:N — 상품은 여러 옵션(사이즈/컬러)을 가짐

users
  │
  ├──< orders (user_id)              1:N — 한 회원이 여러 주문
  │       │
  │       └──< order_items           1:N — 한 주문에 여러 상품
  │               ├── products (FK)
  │               └── product_options (FK)
  │
  ├── carts (user_id)                1:1 — 회원당 장바구니 1개
  │       │
  │       └──< cart_items
  │               ├── products (FK)
  │               └── product_options (FK)
  │
  └──< payments (order_id)           1:1 — 주문당 결제 1건
```

## 주의사항

- 금액 필드(`price`)는 반드시 `BIGINT` (Kotlin `Long`) — 부동소수점 오차 방지
- `ddl-auto: create-drop` 설정으로 개발 중 서버 재시작 시 스키마가 재생성됨
- 운영 배포 시 `validate` 또는 `none`으로 변경 필요
