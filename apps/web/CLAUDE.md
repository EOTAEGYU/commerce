# 프론트엔드 개요
Next.js 16 (App Router) 기반 패션 자사몰 프론트엔드

## 기술 스택
- Framework: Next.js 16.2.3 (App Router, Turbopack)
- Language: TypeScript 5
- Styling: Tailwind CSS 4 (CSS-first, tailwind.config.js 없음)
- 서버 상태: TanStack Query 5 (staleTime 60s, retry 1)
- 클라이언트 상태: Zustand 5 (persist 미들웨어)
- 타입 생성: openapi-typescript 7

## 주요 명령어
- `npm run dev` : 개발 서버 실행 (localhost:3000, Turbopack)
- `npm run build` : 프로덕션 빌드
- `npm run lint` : ESLint 검사
- `npm run generate:types` : OpenAPI → 타입 생성 (루트에서 실행, 백엔드 기동 필요)

## 폴더 구조
```
src/
├── app/                   # Next.js App Router 페이지
│   ├── (shop)/            # 일반 사용자 — 상품, 장바구니, 주문 (3~4단계)
│   ├── (auth)/            # 인증
│   │   ├── signin/page.tsx  # 로그인 페이지
│   │   └── signup/page.tsx  # 회원가입 페이지
│   ├── admin/             # 관리자 전용 (5단계)
│   ├── layout.tsx         # 루트 레이아웃 (Header + main + Footer)
│   ├── page.tsx           # 홈 (3단계에서 상품 목록으로 교체 예정)
│   └── providers.tsx      # Provider 조합 (QueryProvider + AuthHydration)
├── components/
│   ├── Header.tsx         # 상단 네비 (카테고리/장바구니/인증)
│   └── Footer.tsx         # 하단 바
├── lib/
│   └── api/
│       └── client.ts      # apiFetch(), ApiError
├── providers/
│   └── QueryProvider.tsx  # TanStack Query Provider
├── store/
│   └── auth.ts            # Zustand 인증 스토어
└── types/
    ├── api.ts             # ApiResponse<T>, PageResponse<T>, components re-export
    └── api.generated.ts   # 자동 생성 타입 (git 제외, 직접 수정 금지)
```

## API 클라이언트 (`src/lib/api/client.ts`)
- `apiFetch<T>(path, options?)`: 모든 API 요청에 사용
  - `Authorization: Bearer {token}` 자동 첨부 (Zustand store에서 읽음)
  - 401 응답 시 자동 로그아웃 (clearAuth() 호출)
  - 실패 시 `ApiError(code, message)` throw → UI에서 code로 분기 처리 가능
- `ApiError`: `code` (백엔드 ErrorCode), `message` 보존

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
