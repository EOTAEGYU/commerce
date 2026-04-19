# 프론트엔드 기술 스택

## 스택 구성

| 분류 | 기술 | 버전 |
|------|------|------|
| Framework | Next.js | 15.x (App Router) |
| Language | TypeScript | 5.x |
| Styling | Tailwind CSS | 4.x |
| 서버 상태 관리 | TanStack Query (React Query) | 5.x |
| 클라이언트 상태 관리 | Zustand | 5.x |
| 타입 생성 | openapi-typescript | 7.x |

## 선정 이유

### Next.js (App Router)
백엔드 API가 인증 여부에 따라 공개/비공개 엔드포인트로 명확히 분리되어 있어 렌더링 전략을 혼합해서 사용하기 적합하다.

| 페이지 | 렌더링 방식 | 이유 |
|--------|-----------|------|
| 상품 목록 (`/`) | SSR | 검색엔진 노출 필요, 재고/가격 실시간성 |
| 상품 상세 (`/products/[id]`) | SSR | SEO 핵심 페이지 |
| 카테고리 목록 | ISR | 변경 빈도 낮음, 캐싱 이점 |
| 장바구니 (`/cart`) | CSR | JWT 인증 필요, SEO 불필요 |
| 주문/결제 | CSR | 인증 필요, 민감한 흐름 |
| 관리자 (`/admin/**`) | CSR | ADMIN 역할 전용 |

### TypeScript
백엔드 DTO 구조가 `ApiResponse<T>` 공통 래퍼로 고정되어 있고 `openapi-typescript`로 타입을 자동 생성한다. 컴파일 타임에 API 응답 형식 불일치를 잡을 수 있다.

### Tailwind CSS
Next.js 공식 통합을 지원하며 별도 CSS 파일 없이 빠른 레이아웃 구성이 가능하다.

### TanStack Query
- 상품 목록, 장바구니, 주문 데이터의 캐싱과 재요청 자동 처리
- 백엔드 응답이 `Page<T>` 형태이므로 `useInfiniteQuery`로 무한 스크롤 구현 가능
- JWT 토큰 만료 시 401 응답을 인터셉트해 로그아웃 처리 가능

### Zustand
- 로그인 상태(token, user) 전역 관리
- `persist` 미들웨어로 localStorage 동기화 → 새로고침 후에도 로그인 유지
- Redux 대비 보일러플레이트 없음

## 주요 명령어

```bash
# 개발 서버 실행 (http://localhost:3000)
npm run dev

# 프로덕션 빌드
npm run build

# 빌드 결과물 실행
npm run start

# 타입 생성 (백엔드 서버 기동 후 실행)
npm run generate:types   # 루트에서 실행
```

## 환경변수

```env
# apps/web/.env.local
NEXT_PUBLIC_API_URL=http://localhost:8080
```
