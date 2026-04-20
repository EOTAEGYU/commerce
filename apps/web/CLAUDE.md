# 프론트엔드 개요
Next.js 16 (App Router) 기반 패션 자사몰 프론트엔드
기술 스택 개요는 루트 CLAUDE.md 참조. 추가 주의: Tailwind CSS 4는 CSS-first(`tailwind.config.js` 없음), TanStack Query `staleTime: 60_000 / retry: 1`.

## 주요 명령어
- `npm run dev` : 개발 서버 실행 (localhost:3000, webpack — `next dev --webpack`)
- `npm run build` : 프로덕션 빌드
- `npm run lint` : ESLint 검사
- `npm run generate:types` : OpenAPI → 타입 생성 (루트에서 실행, 백엔드 기동 필요)
- `npx jest --no-coverage` : 단위 테스트 실행 (apps/web 내에서)

## 테스트
- 테스트 환경: Jest + @testing-library/react + jest-environment-jsdom
- 설정 파일: `jest.config.js`, `jest.setup.ts`
- 테스트 위치: `src/__tests__/*.test.tsx`
- Next.js 모듈 mock: `src/__mocks__/next/` (link, image, navigation)
- `useRouter`, `usePathname`, `useSearchParams` → jest.fn()으로 mock 처리

## 폴더 구조
```
src/
├── app/                   # Next.js App Router 페이지
│   ├── (auth)/            # 인증 라우트 그룹
│   │   ├── signin/page.tsx  # 로그인 페이지
│   │   └── signup/page.tsx  # 회원가입 페이지
│   ├── products/
│   │   └── [id]/page.tsx  # 상품 상세 (SSR) — 갤러리 그리드 + sticky 패널
│   ├── categories/
│   │   └── [id]/page.tsx  # 카테고리 목록 (SSR) — 좌측 필터 사이드바 + 4열 그리드
│   ├── search/
│   │   └── page.tsx       # 검색 결과 (SSR) — 필터 사이드바 + 4열 그리드
│   ├── cart/
│   │   └── page.tsx       # 장바구니 (CSR)
│   ├── orders/
│   │   ├── page.tsx       # 주문 목록 (CSR)
│   │   └── [id]/page.tsx  # 주문 상세 + 결제 (CSR)
│   ├── admin/             # 관리자 전용
│   ├── layout.tsx         # 루트 레이아웃 (Header + main + Footer)
│   ├── page.tsx           # 홈 (SSR) — HeroBanner + New Arrivals + Best Sellers + Category 배너
│   └── providers.tsx      # Provider 조합 (QueryProvider + AuthHydration)
├── components/
│   ├── home/              # 홈 전용 컴포넌트
│   │   ├── HeroBanner.tsx        # 3슬라이드 자동재생 캐러셀 (클라이언트)
│   │   ├── HomeSection.tsx       # 섹션 헤더 + 5열 상품 그리드 (서버)
│   │   └── CategoryBanners.tsx   # 4열 카테고리 배너 (서버)
│   ├── search/            # 검색 컴포넌트
│   │   └── SearchOverlay.tsx     # 전체화면 검색 오버레이 (클라이언트)
│   ├── layout/            # 레이아웃 컴포넌트
│   │   └── Breadcrumb.tsx        # 브레드크럼 (서버)
│   ├── products/          # 상품 관련 컴포넌트
│   │   ├── ProductCard.tsx        # 상품 카드 (서버)
│   │   ├── ProductGrid.tsx        # 상품 그리드 (서버)
│   │   ├── FilterSidebar.tsx      # 좌측 필터 (카테고리/사이즈/컬러/가격, 클라이언트)
│   │   ├── AppliedFilters.tsx     # 적용된 필터 칩 (클라이언트)
│   │   ├── SortDropdown.tsx       # 정렬 드롭다운 (클라이언트)
│   │   ├── Accordion.tsx          # 아코디언 (클라이언트, PDP용)
│   │   ├── CategoryFilter.tsx     # 카테고리 pill 필터 (클라이언트, 레거시)
│   │   ├── PaginationBar.tsx      # 페이지네이션 (클라이언트)
│   │   ├── ProductOptionPicker.tsx  # 옵션 선택 (클라이언트)
│   │   └── AddToCartButton.tsx    # 장바구니 담기 mutation (클라이언트)
│   ├── cart/
│   │   └── CartItemRow.tsx        # 장바구니 아이템 행 (클라이언트)
│   ├── Header.tsx         # 상단 네비 + SearchOverlay 통합 (카테고리 GNB → /categories/[id])
│   └── Footer.tsx         # 하단 바
├── __tests__/             # 단위 테스트 (Jest + @testing-library/react)
│   ├── Breadcrumb.test.tsx
│   ├── HeroBanner.test.tsx
│   ├── HomeSection.test.tsx
│   ├── CategoryBanners.test.tsx
│   ├── SearchOverlay.test.tsx
│   ├── FilterSidebar.test.tsx
│   ├── AppliedFilters.test.tsx
│   └── SortDropdown.test.tsx
├── __mocks__/next/        # Next.js 모듈 mock (테스트용)
│   ├── link.tsx, image.tsx, navigation.ts
├── lib/
│   └── api/
│       ├── client.ts      # apiFetch(), ApiError (클라이언트 전용)
│       └── server.ts      # serverFetch() (서버 컴포넌트 전용)
├── providers/
│   └── QueryProvider.tsx  # TanStack Query Provider
├── store/
│   └── auth.ts            # Zustand 인증 스토어
└── types/
    ├── api.ts             # ApiResponse<T>, PageResponse<T>, components re-export
    └── api.generated.ts   # 자동 생성 타입 (git 제외, 직접 수정 금지)
```

## API 클라이언트

### `src/lib/api/client.ts` — 클라이언트 컴포넌트 전용
- `apiFetch<T>(path, options?)`: 클라이언트 컴포넌트에서 모든 API 요청에 사용
  - `Authorization: Bearer {token}` 자동 첨부 (Zustand store에서 읽음)
  - 401 응답 시 자동 로그아웃 (clearAuth() 호출)
  - 실패 시 `ApiError(code, message)` throw → UI에서 code로 분기 처리 가능
- `ApiError`: `code` (백엔드 ErrorCode), `message` 보존

### `src/lib/api/server.ts` — 서버 컴포넌트 전용
- `serverFetch<T>(path, options?)`: 서버 컴포넌트(SSR)에서 API 요청에 사용
  - Zustand 미사용 (서버에서 실행되므로) → 공개 API 전용
  - 실패 시 `Error` throw → 호출부에서 try/catch 또는 `notFound()` 처리
- **주의**: 인증이 필요한 API는 서버에서 호출 불가 → 클라이언트 컴포넌트에서 `apiFetch` 사용

## 상태 관리

### Zustand 인증 스토어 (`src/store/auth.ts`)
- `token`: JWT 액세스 토큰
- `user`: `{ id, email, name, role: 'USER' | 'ADMIN' }`
- `setAuth(token, user)`: 로그인 성공 시 호출 (백엔드 AuthResponse.accessToken 사용)
- `clearAuth()`: 로그아웃 또는 401 시 호출
- `skipHydration: true` 설정 → SSR hydration mismatch 방지
  - 클라이언트 컴포넌트에서 `useEffect(() => { useAuthStore.persist.rehydrate() }, [])` 필요

### TanStack Query
- `QueryProvider.tsx`에서 QueryClient 생성 (useState로 — 서버/클라이언트 격리)
- 개발 환경에서 ReactQueryDevtools 활성화 (우측 하단)

## 코드 컨벤션 (IMPORTANT)
- 모든 클라이언트 훅/상태 사용 파일 최상단에 `'use client'` 선언
- Server Component에서 `apiFetch` 사용 시 토큰은 항상 null (쿠키 기반 인증 별도 처리)
- Next.js 15+는 기본 fetch 캐싱 없음 → 캐싱 필요 시 `{ next: { revalidate: N } }` 명시
- Tailwind 4: `@import "tailwindcss"` 방식 사용 (`tailwind.config.js` 없음)
- 금액 표시: `toLocaleString('ko-KR')` 원 단위 정수 포맷

## 인증 흐름
1. `/signin` → `POST /api/users/signin` → `accessToken` 획득
2. `GET /api/users/me` (Authorization 헤더 직접 첨부) → user 정보 획득
3. `setAuth(token, user)` → Zustand persist로 localStorage 저장
4. `providers.tsx`의 `AuthHydration`이 마운트 시 `rehydrate()` 호출 → 새로고침 후에도 로그인 유지
5. 401 응답 → `apiFetch` 내부에서 자동 `clearAuth()` 호출

## 백엔드 연동 주의사항
- 개발 서버 포트: **3000** (백엔드 CORS가 localhost:3000 허용)
- API Base URL: `NEXT_PUBLIC_API_URL=http://localhost:8080`
- 로그인 응답 토큰 필드: `accessToken` (AuthResponse.accessToken)
- 백엔드 ErrorCode는 `ApiError.code`로 접근 가능
