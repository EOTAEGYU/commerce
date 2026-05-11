# 프론트엔드 (apps/web/)

## 추가 명령어
- `npx jest --no-coverage` : 단위 테스트 (apps/web 내에서)

## API 클라이언트
- **클라이언트 컴포넌트**: `apiFetch<T>(path, options?)` — Authorization 자동 첨부, 401 시 자동 로그아웃
- **서버 컴포넌트**: `serverFetch<T>(path, options?)` — Zustand 미사용, 공개 API 전용
- 인증 필요 API는 서버에서 호출 불가 → 반드시 클라이언트 컴포넌트에서 `apiFetch` 사용

## 상태 관리
- Zustand `skipHydration: true` → 클라이언트 컴포넌트에서 `useEffect(() => { useAuthStore.persist.rehydrate() }, [])` 필요
- TanStack Query 설정: `staleTime: 60_000`, `retry: 1`

## 코드 컨벤션
- 클라이언트 훅/상태 사용 파일: 최상단에 `'use client'` 선언
- Tailwind 4: `@import "tailwindcss"` 방식 (`tailwind.config.js` 없음)
- 캐싱 필요 시: `{ next: { revalidate: N } }` 명시 (Next.js 기본 캐싱 없음)
- 금액 표시: `toLocaleString('ko-KR')`

## 테스트
- 위치: `src/__tests__/`, Next.js 모듈 mock: `src/__mocks__/next/`
- `useRouter`, `usePathname`, `useSearchParams` → `jest.fn()`으로 mock

## 백엔드 연동
- API Base URL: `NEXT_PUBLIC_API_URL=http://localhost:8080`
- CORS: localhost:3000만 허용 (포트 변경 금지)
- 로그인 응답 토큰 필드: `accessToken` (`ApiError.code`로 백엔드 ErrorCode 접근)
