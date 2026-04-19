# 프론트엔드 아키텍처

## 폴더 구조

```
apps/web/
├── public/                    # 정적 파일 (이미지, 아이콘)
├── src/
│   ├── app/                   # Next.js App Router 라우트
│   │   ├── (auth)/            # 인증 라우트 그룹
│   │   │   ├── signin/
│   │   │   │   └── page.tsx   # /signin — 로그인
│   │   │   └── signup/
│   │   │       └── page.tsx   # /signup — 회원가입
│   │   ├── products/
│   │   │   └── [id]/
│   │   │       └── page.tsx   # /products/[id] — 상품 상세 (SSR)
│   │   ├── cart/
│   │   │   └── page.tsx       # /cart — 장바구니 (CSR)
│   │   ├── orders/
│   │   │   ├── page.tsx       # /orders — 주문 내역 (CSR)
│   │   │   └── [id]/
│   │   │       └── page.tsx   # /orders/[id] — 주문 상세 + 결제 (CSR)
│   │   ├── admin/             # 관리자 (ADMIN 역할 전용, 5단계)
│   │   │   ├── products/
│   │   │   │   └── page.tsx   # 상품 관리
│   │   │   └── categories/
│   │   │       └── page.tsx   # 카테고리 관리
│   │   ├── layout.tsx         # 루트 레이아웃 (Header + main + Footer)
│   │   ├── page.tsx           # / — 상품 목록 (SSR, 카테고리 필터, 페이지네이션)
│   │   └── providers.tsx      # QueryClient, Zustand 등 Provider 모음
│   ├── components/            # 재사용 컴포넌트
│   │   ├── products/          # 상품 관련
│   │   │   ├── ProductCard.tsx        # 상품 카드 (서버 컴포넌트)
│   │   │   ├── ProductGrid.tsx        # 상품 그리드 + 빈 상태 (서버 컴포넌트)
│   │   │   ├── CategoryFilter.tsx     # 카테고리 pill 필터 (클라이언트)
│   │   │   ├── PaginationBar.tsx      # 페이지네이션 (클라이언트)
│   │   │   ├── ProductOptionPicker.tsx  # 옵션 선택 (클라이언트)
│   │   │   └── AddToCartButton.tsx    # 장바구니 담기 mutation (클라이언트)
│   │   ├── cart/
│   │   │   └── CartItemRow.tsx        # 장바구니 아이템 행 — 수량변경/삭제 (클라이언트)
│   │   ├── Header.tsx         # 상단 네비 (카테고리 / 주문내역 / 장바구니 뱃지 / 인증)
│   │   └── Footer.tsx         # 하단 바
│   ├── lib/
│   │   └── api/
│   │       ├── client.ts      # apiFetch(), ApiError — 클라이언트 컴포넌트 전용
│   │       └── server.ts      # serverFetch() — 서버 컴포넌트(SSR) 전용
│   ├── providers/
│   │   └── QueryProvider.tsx  # TanStack Query Provider
│   ├── store/
│   │   └── auth.ts            # Zustand 인증 스토어 (persist)
│   └── types/
│       ├── api.generated.ts   # openapi-typescript 자동 생성 (gitignore)
│       └── api.ts             # 공통 래퍼 타입 + 생성 타입 재수출
└── package.json
```

## 라우팅 전략

| 경로 | 렌더링 | 인증 | 설명 |
|------|--------|------|------|
| `/` | SSR | 불필요 | 상품 목록, 카테고리 필터, 페이지네이션 |
| `/products/[id]` | SSR | 불필요 | 상품 상세, 옵션 선택, 장바구니 담기 버튼 |
| `/signin`, `/signup` | CSR | 불필요 | 로그인/회원가입 폼 |
| `/cart` | CSR | 필요 | 장바구니 목록, 수량 변경, 주문하기 |
| `/orders` | CSR | 필요 | 내 주문 내역 |
| `/orders/[id]` | CSR | 필요 | 주문 상세 + 결제 진행 + 주문 취소 |
| `/admin/**` | CSR | ADMIN | 상품/카테고리 관리 (5단계) |

## 서버 / 클라이언트 컴포넌트 분리 원칙

- **서버 컴포넌트 (기본)**: 초기 데이터 페칭, SEO 필요 페이지, 순수 display 컴포넌트
- **클라이언트 컴포넌트 (`'use client'`)**: 브라우저 상태/훅 필요 시 최소 범위로 지정
  - URL params 읽기/쓰기 (`useSearchParams`, `useRouter`)
  - 사용자 인터랙션 (클릭, 폼 입력)
  - React Query mutation (`useMutation`)
  - Zustand store 접근

## API 클라이언트 설계

### `src/lib/api/client.ts` — 클라이언트 전용
```typescript
export class ApiError extends Error {
  constructor(public readonly code: string, message: string) { ... }
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T>
// - Zustand store에서 JWT 토큰 자동 첨부
// - 401 시 clearAuth() 자동 호출
// - 실패 시 ApiError(code, message) throw → UI에서 code로 분기 처리
```

### `src/lib/api/server.ts` — 서버 컴포넌트 전용
```typescript
export async function serverFetch<T>(path: string, options?: RequestInit): Promise<T>
// - 공개 API 전용 (인증 토큰 없음)
// - 실패 시 Error throw → 호출부에서 try/catch 또는 notFound() 처리
```

## 상태 관리 전략

### Zustand — 클라이언트 전역 상태
인증 정보처럼 서버와 무관하게 브라우저에서만 관리하는 상태.

```typescript
// src/store/auth.ts
type AuthStore = {
  token: string | null
  user: { id: number; email: string; name: string; role: 'USER' | 'ADMIN' } | null
  setAuth: (token: string, user: AuthStore['user']) => void
  clearAuth: () => void
}
// persist 미들웨어로 localStorage 자동 저장
// skipHydration: true → SSR hydration mismatch 방지
// providers.tsx의 AuthHydration 컴포넌트에서 마운트 시 rehydrate() 호출
```

### TanStack Query — 서버 상태
API로 가져오는 데이터 (장바구니, 주문 등 클라이언트 조회).

```typescript
// queryKey 규칙
['categories']           // 카테고리 목록 (Header에서 사용, staleTime 5분)
['cart']                 // 장바구니 (Header 뱃지 + 장바구니 페이지)
['product', id]          // 상품 단건 (장바구니 상품명 표시용, staleTime 5분)
['orders']               // 주문 목록
['order', id]            // 주문 단건 상세
['payment', orderId]     // 결제 정보 (order.status !== 'PENDING'일 때만 enabled)

// setQueryData vs invalidateQueries
// - Cart 뮤테이션: 서버가 CartResponse 전체 반환 → setQueryData로 즉시 교체
// - 주문 취소: OrderResponse 반환 → setQueryData(['order', id]) + invalidate(['orders'])
// - 결제 성공: invalidate(['order', id]) → 상태 변경 후 payment 쿼리 자동 활성화
```

## SSR 데이터 페칭 패턴

서버 컴포넌트에서 `searchParams` / `params`는 Next.js 15+ 기준 Promise이므로 반드시 await 처리.

```typescript
// app/page.tsx
export default async function HomePage({
  searchParams,
}: {
  searchParams: Promise<{ categoryId?: string; page?: string }>
}) {
  const { categoryId, page } = await searchParams
  const [categories, productsPage] = await Promise.all([
    serverFetch<CategoryResponse[]>('/api/categories'),
    serverFetch<PageProductResponse>(`/api/products?${params}`),
  ])
  // ...
}
```

## URL 기반 필터/페이지네이션 패턴

- 필터 상태는 URL search params에만 저장 (`useState` 미사용)
- `/?categoryId=3&page=2` 형태 (page는 0-indexed)
- 카테고리 변경 시 page 파라미터 리셋 (첫 페이지로)
- 북마크/공유 가능, 새로고침 후에도 상태 유지

```typescript
// 클라이언트 컴포넌트에서 URL 변경
router.push(`/?categoryId=${id}`)  // 카테고리 선택
router.push('/')                    // 전체 선택 (파라미터 제거)
```
