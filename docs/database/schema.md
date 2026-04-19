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

## categories

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

## products

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

## product_options

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

## carts

회원당 1개 장바구니. 주문 완료 시 아이템 비워짐.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 장바구니 ID |
| `user_id` | BIGINT | NOT NULL, UNIQUE | 회원 ID (1:1) |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

---

## cart_items

장바구니에 담긴 상품 항목.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 항목 ID |
| `cart_id` | BIGINT | NOT NULL, FK(carts) | 장바구니 ID |
| `product_id` | BIGINT | NOT NULL | 상품 ID |
| `product_option_id` | BIGINT | NOT NULL | 옵션 ID (사이즈/컬러) |
| `quantity` | INT | NOT NULL | 수량 |
| `price` | BIGINT | NOT NULL | 담을 당시 가격 스냅샷 |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

---

## orders

주문 헤더 테이블.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 주문 ID |
| `user_id` | BIGINT | NOT NULL | 회원 ID |
| `status` | VARCHAR | NOT NULL | 주문 상태 (`PENDING`, `PAID`, `SHIPPING`, `DELIVERED`, `CANCELLED`) |
| `total_amount` | BIGINT | NOT NULL | 주문 총액 (원 단위) |
| `created_at` | TIMESTAMP | NOT NULL | 주문 시각 (10분 만료 기준) |
| `updated_at` | TIMESTAMP | NOT NULL | |

---

## order_items

주문 시점 상품 스냅샷 테이블. 이후 상품 정보 변경에 영향받지 않음.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 항목 ID |
| `order_id` | BIGINT | NOT NULL, FK(orders) | 주문 ID |
| `product_id` | BIGINT | NOT NULL | 상품 ID (참조용) |
| `product_option_id` | BIGINT | NOT NULL | 옵션 ID (참조용) |
| `product_name` | VARCHAR | NOT NULL | 주문 시점 상품명 스냅샷 |
| `option_info` | VARCHAR | NOT NULL | 주문 시점 옵션 정보 스냅샷 ("M / 블랙") |
| `price` | BIGINT | NOT NULL | 주문 시점 가격 스냅샷 |
| `quantity` | INT | NOT NULL | 수량 |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

---

## payments

결제 내역 테이블. 주문당 1건.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 결제 ID |
| `order_id` | BIGINT | NOT NULL, UNIQUE, FK(orders) | 주문 ID (1:1) |
| `user_id` | BIGINT | NOT NULL | 회원 ID |
| `amount` | BIGINT | NOT NULL | 결제 금액 |
| `method` | VARCHAR | NOT NULL | 결제 수단 (`CARD`, `BANK_TRANSFER`) |
| `status` | VARCHAR | NOT NULL | 결제 상태 (`REQUESTED`, `COMPLETED`, `FAILED`, `REFUNDED`) |
| `pg_transaction_id` | VARCHAR | | PG사 거래 ID (성공 시 저장) |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

---

---

## reviews

상품 리뷰 테이블. OrderItem 단위로 1개 리뷰 작성 가능.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 리뷰 ID |
| `user_id` | BIGINT | NOT NULL | 작성자 회원 ID |
| `product_id` | BIGINT | NOT NULL | 상품 ID (통계 조회용) |
| `order_id` | BIGINT | NOT NULL | 주문 ID |
| `order_item_id` | BIGINT | NOT NULL, UNIQUE | 주문 항목 ID (중복 방지) |
| `rating` | DOUBLE | NOT NULL | 별점 (0.5 ~ 5.0, 0.5단위) |
| `content` | TEXT | NOT NULL | 리뷰 본문 (최대 500자) |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

---

## product_likes

상품 좋아요(찜) 테이블. 회원-상품 쌍에 유니크 제약.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 좋아요 ID |
| `user_id` | BIGINT | NOT NULL, INDEX | 회원 ID |
| `product_id` | BIGINT | NOT NULL, INDEX | 상품 ID |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

- `(user_id, product_id)` UNIQUE 제약 — 동일 상품 중복 좋아요 방지
- `idx_product_likes_user_id` — 내 좋아요 목록 조회 최적화
- `idx_product_likes_product_id` — 상품별 좋아요 수 집계 최적화

---

## coupon_templates

관리자가 정의하는 쿠폰 정책 테이블.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 쿠폰 템플릿 ID |
| `name` | VARCHAR | NOT NULL | 쿠폰명 |
| `discount_type` | VARCHAR | NOT NULL | FIXED(정액) / RATE(정률) |
| `discount_value` | BIGINT | NOT NULL | 정액: 원 단위 / 정률: % 값 |
| `max_discount_amount` | BIGINT | NULLABLE | 정률 쿠폰 최대 할인 한도 |
| `min_order_amount` | BIGINT | NULLABLE | 최소 주문금액 조건 |
| `category_id` | BIGINT | NULLABLE | 특정 카테고리 제한 (null=전체) |
| `total_quantity` | INT | NULLABLE | 발급 수량 한도 (null=무제한) |
| `issued_count` | INT | NOT NULL, DEFAULT 0 | 현재까지 발급된 수량 |
| `valid_from` | TIMESTAMP | NOT NULL | 쿠폰 사용 시작일 |
| `valid_until` | TIMESTAMP | NOT NULL | 쿠폰 사용 만료일 |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT true | 관리자 활성화 여부 |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Optimistic Lock 버전 |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

---

## user_coupons

사용자가 발급받은 쿠폰 인스턴스 테이블.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 사용자 쿠폰 ID |
| `user_id` | BIGINT | NOT NULL, INDEX | 발급받은 회원 ID |
| `coupon_template_id` | BIGINT | NOT NULL | 쿠폰 템플릿 ID |
| `status` | VARCHAR | NOT NULL, DEFAULT 'UNUSED' | UNUSED / USED / EXPIRED |
| `used_order_id` | BIGINT | NULLABLE | 사용한 주문 ID |
| `used_at` | TIMESTAMP | NULLABLE | 사용 시각 |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

- `(user_id, coupon_template_id)` UNIQUE 제약 — 동일 쿠폰 중복 발급 방지

---

## settlements

일별 매출 정산 테이블. 날짜당 1건만 허용.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO INCREMENT | 정산 ID |
| `settlement_date` | DATE | NOT NULL, UNIQUE | 정산 대상 날짜 |
| `status` | VARCHAR | NOT NULL, DEFAULT 'PENDING' | PENDING / CONFIRMED / PAID |
| `order_count` | INT | NOT NULL | 해당 날짜 COMPLETED 결제 건수 |
| `total_order_amount` | BIGINT | NOT NULL | 총 주문금액 (실결제액 + 할인 + 포인트 역산) |
| `total_discount_amount` | BIGINT | NOT NULL | 총 쿠폰 할인액 |
| `total_point_amount` | BIGINT | NOT NULL | 총 포인트 사용액 |
| `total_net_amount` | BIGINT | NOT NULL | 순매출 (실결제액 합계) |
| `total_earned_points` | BIGINT | NOT NULL | 총 적립 포인트 |
| `confirmed_at` | TIMESTAMP | NULLABLE | CONFIRMED 전이 시각 |
| `paid_at` | TIMESTAMP | NULLABLE | PAID 전이 시각 |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

---

## 엔티티 관계도

```
categories
  │
  └──< products (category_id)              N:1 — 상품은 하나의 소분류에 속함
          │
          ├──< product_options             1:N — 상품은 여러 옵션(사이즈/컬러)을 가짐
          │
          ├──< reviews (product_id)        1:N — 상품별 리뷰 목록 조회용
          │
          └──< product_likes (product_id)  1:N — 상품별 좋아요

users
  │
  ├──< orders (user_id)                   1:N — 한 회원이 여러 주문
  │       │
  │       ├──< order_items                1:N — 한 주문에 여러 상품 (스냅샷)
  │       │       │
  │       │       └── reviews (order_item_id, UNIQUE)  1:1 — 항목당 리뷰 1개
  │       │
  │       └── payments (order_id)         1:1 — 주문당 결제 1건
  │
  ├──< reviews (user_id)                  1:N — 회원이 작성한 리뷰 목록
  │
  ├──< product_likes (user_id)            1:N — 회원이 찜한 상품 목록
  │
  ├──< user_coupons (user_id)             1:N — 회원이 발급받은 쿠폰
  │
  └── carts (user_id, UNIQUE)             1:1 — 회원당 장바구니 1개
          │
          └──< cart_items                 1:N — 장바구니에 여러 상품
```

## 주의사항

- 금액 필드(`price`, `total_amount`, `amount`)는 반드시 `BIGINT` (Kotlin `Long`) — 부동소수점 오차 방지
- `ddl-auto: create-drop` 설정으로 개발 중 서버 재시작 시 스키마가 재생성됨
- 운영 배포 시 `validate` 또는 `none`으로 변경 필요
