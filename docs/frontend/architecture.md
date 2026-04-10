# 프론트엔드 아키텍처

## 폴더 구조

```
apps/web/
├── public/                    # 정적 파일 (이미지, 아이콘)
├── src/
│   ├── app/                   # Next.js App Router 라우트
│   │   ├── (shop)/            # 일반 사용자 레이아웃 그룹
│   │   │   ├── page.tsx       # / — 상품 목록
│   │   │   ├── products/
│   │   │   │   └── [id]/
│   │   │   │       └── page.tsx  # /products/[id] — 상품 상세
│   │   │   ├── cart/
│   │   │   │   └── page.tsx   # /cart — 장바구니
│   │   │   └── orders/
│   │   │       ├── page.tsx   # /orders — 주문 내역
│   │   │       └── [id]/
│   │   │           └── page.tsx  # /orders/[id] — 주문 상세
│   │   ├── (auth)/            # 인증 레이아웃 그룹
│   │   │   ├── signin/
│   │   │   │   └── page.tsx   # /signin — 로그인
│   │   │   └── signup/
│   │   │       └── page.tsx   # /signup — 회원가입
│   │   ├── admin/             # 관리자 (ADMIN 역할 전용)
│   │   │   ├── layout.tsx     # ADMIN 권한 체크 레이아웃
│   │   │   ├── products/
│   │   │   │   └── page.tsx   # 상품 관리
│   │   │   └── categories/
│   │   │       └── page.tsx   # 카테고리 관리
│   │   ├── layout.tsx         # 루트 레이아웃
│   │   └── providers.tsx      # QueryClient, Zustand 등 Provider 모음
│   ├── components/            # 재사용 컴포넌트
│   │   ├── ui/                # 버튼, 인풋 등 원자 컴포넌트
│   │   ├── product/           # 상품 관련 컴포넌트
│   │   ├── cart/              # 장바구니 컴포넌트
│   │   └── layout/            # Header, Footer, Nav
│   ├── lib/
│   │   ├── api/               # API 클라이언트 및 도메인별 함수
│   │   │   ├── client.ts      # fetch 래퍼 (토큰 자동 첨부)
│   │   │   ├── products.ts    # 상품 API 함수
│   │   │   ├── cart.ts        # 장바구니 API 함수
│   │   │   ├── orders.ts      # 주문 API 함수
│   │   │   └── payments.ts    # 결제 API 함수
│   │   └── query-keys.ts      # React Query 키 상수 모음
│   ├── store/
│   │   └── auth.ts            # Zustand 인증 스토어
│   └── types/
│       ├── api.generated.ts   # openapi-typescript 자동 생성 (gitignore)
│       └── api.ts             # 공통 래퍼 타입 + 생성 타입 재수출
└── package.json
```

## 라우팅 전략

Next.js App Router의 Route Group을 활용해 레이아웃을 분리한다.

| 경로 | 렌더링 | 인증 | 설명 |
|------|--------|------|------|
| `/` | SSR | 불필요 | 상품 목록, 카테고리 필터, 페이지네이션 |
| `/products/[id]` | SSR | 불필요 | 상품 상세, 옵션 선택, 장바구니 담기 버튼 |
| `/signin`, `/signup` | CSR | 불필요 | 로그인/회원가입 폼 |
| `/cart` | CSR | 필요 | 장바구니 목록, 수량 변경, 주문하기 |
| `/orders` | CSR | 필요 | 내 주문 내역 |
| `/orders/[id]` | CSR | 필요 | 주문 상세 + 결제 진행 |
| `/admin/**` | CSR | ADMIN | 상품/카테고리 관리 |

## 상태 관리 전략

두 가지 상태를 역할에 따라 분리한다.

### Zustand — 클라이언트 전역 상태

인증 정보처럼 서버와 무관하게 브라우저에서만 관리하는 상태.

```typescript
// src/store/auth.ts
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

type AuthStore = {
  token: string | null
  user: { id: number; email: string; name: string; role: string } | null
  setAuth: (token: string, user: AuthStore['user']) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setAuth: (token, user) => set({ token, user }),
      clearAuth: () => set({ token: null, user: null }),
    }),
    { name: 'auth-storage' }  // localStorage에 자동 저장
  )
)
```

### TanStack Query — 서버 상태

상품 목록, 장바구니, 주문 등 API로 가져오는 데이터.

```typescript
// 사용 예시
const { data } = useQuery({
  queryKey: queryKeys.products.list({ categoryId, page }),
  queryFn: () => getProducts({ categoryId, page }),
})
```

## API 클라이언트 설계

모든 API 호출은 `client.ts`의 래퍼를 통해 이루어진다. JWT 토큰 자동 첨부와 401 처리를 중앙화한다.

```typescript
// src/lib/api/client.ts
const BASE_URL = process.env.NEXT_PUBLIC_API_URL

export async function apiFetch<T>(
  path: string,
  options?: RequestInit
): Promise<T> {
  const token = useAuthStore.getState().token

  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })

  if (res.status === 401) {
    useAuthStore.getState().clearAuth()
    throw new Error('UNAUTHORIZED')
  }

  const body = await res.json()
  if (!body.success) throw new Error(body.error)
  return body.data
}
```

## React Query 키 관리

```typescript
// src/lib/query-keys.ts
export const queryKeys = {
  products: {
    all: ['products'] as const,
    list: (params: { categoryId?: number; page?: number }) =>
      ['products', 'list', params] as const,
    detail: (id: number) => ['products', id] as const,
  },
  cart: {
    me: ['cart'] as const,
  },
  orders: {
    all: ['orders'] as const,
    detail: (id: number) => ['orders', id] as const,
  },
}
```
