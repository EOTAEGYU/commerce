# API 타입 자동 생성

백엔드 Swagger 스펙(`/v3/api-docs`)에서 TypeScript 타입을 자동 생성하는 파이프라인이다.

## 도구

**`openapi-typescript`** — OpenAPI 3.x 스펙을 TypeScript 타입으로 변환.
런타임 의존성 없이 타입 정의 파일만 생성한다.

## 실행 방법

```bash
# 1. 백엔드 서버 기동 (별도 터미널)
./gradlew :apps:api:bootRun

# 2. 루트 디렉토리에서 타입 생성
npm run generate:types
# → apps/web/src/types/api.generated.ts 파일 생성
```

## 파일 구조

```
apps/web/src/types/
├── api.generated.ts   # 자동 생성 — 직접 수정 금지, .gitignore 처리
└── api.ts             # 수동 작성 — 공통 래퍼 타입 및 재수출
```

`api.generated.ts`는 매번 덮어쓰이므로 직접 수정하지 않는다.
도메인 타입은 `api.generated.ts`에서 import해서 사용한다.

## 공통 래퍼 타입 (api.ts)

백엔드의 `ApiResponse<T>` 래퍼와 Spring의 `Page<T>` 응답은 openapi-typescript로 자동 생성되지 않으므로 수동 정의한다.

```typescript
// apps/web/src/types/api.ts

// 모든 API 응답의 공통 래퍼 (백엔드 ApiResponse<T> 대응)
export type ApiResponse<T> = {
  success: boolean
  data: T
  error: string | null
}

// 상품 목록 등 페이지네이션 응답 (백엔드 Page<T> 대응)
export type PageResponse<T> = {
  content: T[]
  totalPages: number
  totalElements: number
  number: number   // 현재 페이지 (0-indexed)
  size: number
  first: boolean
  last: boolean
}

// 자동 생성 타입 재수출
export type { components } from './api.generated'
```

## 생성 타입 사용 예시

```typescript
import type { components } from '@/types/api.generated'
import type { ApiResponse, PageResponse } from '@/types/api'

// 자동 생성된 스키마 타입 참조
type ProductResponse = components['schemas']['ProductResponse']
type OrderResponse = components['schemas']['OrderResponse']

// API 함수에서 사용
async function getProduct(id: number): Promise<ProductResponse> {
  const res = await apiFetch<ApiResponse<ProductResponse>>(`/api/products/${id}`)
  return res.data
}

async function getProducts(params: {
  categoryId?: number
  page?: number
  size?: number
}): Promise<PageResponse<ProductResponse>> {
  const query = new URLSearchParams()
  if (params.categoryId) query.set('categoryId', String(params.categoryId))
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))

  const res = await apiFetch<ApiResponse<PageResponse<ProductResponse>>>(
    `/api/products?${query}`
  )
  return res.data
}
```

## 주요 생성 타입 목록

백엔드 DTO 기준으로 아래 스키마가 생성된다.

| 스키마명 | 설명 |
|---------|------|
| `UserResponse` | 회원 정보 (id, email, name, role) |
| `AuthResponse` | 로그인 응답 (token) |
| `SignUpRequest` | 회원가입 요청 |
| `SignInRequest` | 로그인 요청 |
| `CategoryResponse` | 카테고리 (id, name, parentId, children) |
| `ProductResponse` | 상품 (id, name, price, imageUrl, options) |
| `ProductOptionResponse` | 상품 옵션 (id, size, color, stock) |
| `ProductCreateRequest` | 상품 등록 요청 |
| `ProductUpdateRequest` | 상품 수정 요청 |
| `CartResponse` | 장바구니 (id, items, totalAmount) |
| `CartItemAddRequest` | 장바구니 담기 요청 |
| `CartItemUpdateRequest` | 장바구니 수량 변경 요청 |
| `OrderResponse` | 주문 (id, status, totalAmount, items) |
| `PaymentRequest` | 결제 요청 (orderId, method) |
| `PaymentResponse` | 결제 결과 (id, status, pgTransactionId) |

## 타입 재생성 시점

백엔드 API에 변경이 생겼을 때 재실행한다.

- 새 엔드포인트 추가
- DTO 필드 추가/수정/삭제
- 응답 구조 변경

```bash
# 백엔드 변경 후 재생성
npm run generate:types
```
