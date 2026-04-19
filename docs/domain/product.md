# 상품 도메인 (Product)

> **구현 상태**: 설계 완료, 코드 미구현

## 개요

의류/신발 패션 자사몰의 상품 등록, 조회, 수정, 삭제 및 옵션별 재고 관리를 담당합니다.

## 비즈니스 규칙

- 상품 등록/수정/삭제는 `ADMIN` 권한을 가진 회원만 가능하다.
- 상품 조회(목록, 상세)는 누구나 가능하다 (인증 불필요).
- 재고는 ProductOption(사이즈+컬러 조합) 단위로 관리한다.
- 재고(stock)는 0 이상이어야 한다.
- 재고 차감은 주문 시점에 Service 레이어에서 Pessimistic Lock으로 처리한다.
- 재고 부족 시 `OUT_OF_STOCK` 예외를 던진다.
- 가격(price)은 Long 타입, 원(KRW) 단위 정수.
- 상품은 반드시 카테고리에 속해야 한다.

## 주요 유스케이스

### 1. 상품 등록
1. ADMIN 권한 확인
2. 카테고리 존재 여부 확인
3. 가격 유효성 검사 (0 이상)
4. Product 엔티티 저장
5. ProductOption 목록 저장 (사이즈/컬러/재고)

### 2. 상품 목록 조회
- 페이지네이션 적용
- 카테고리 ID 필터링 가능
- 전체 공개 (인증 불필요)

### 3. 상품 상세 조회
- 상품 ID로 단건 조회 + 옵션 목록 포함
- 없으면 `PRODUCT_NOT_FOUND` 예외

### 4. 재고 차감 (주문 연동)
1. ProductOption 조회 (Pessimistic Lock)
2. 재고 부족 시 `OUT_OF_STOCK` 예외
3. 재고 차감 후 저장

## 엔티티 필드

### Product
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 상품 ID |
| name | String | 상품명 |
| description | String? | 상품 설명 |
| price | Long | 가격 (원 단위) |
| categoryId | Long | 카테고리 ID |

### ProductOption
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 옵션 ID |
| product | Product | 상품 (FK) |
| size | String | 사이즈 (S/M/L/XL 등) |
| color | String | 컬러 (블랙/화이트 등) |
| stock | Int | 재고 수량 |

## 에러 코드

| 코드 | HTTP | 상황 |
|------|------|------|
| `PRODUCT_NOT_FOUND` | 404 | 존재하지 않는 상품 조회 |
| `PRODUCT_OPTION_NOT_FOUND` | 404 | 존재하지 않는 옵션 조회 |
| `OUT_OF_STOCK` | 409 | 재고 부족 |
