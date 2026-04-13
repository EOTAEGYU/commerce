# TODO (2026-04-11)

## 완료 — 백엔드

- [x] 공통 모듈 (BaseEntity, ApiResponse, ErrorCode, CustomException, GlobalExceptionHandler)
- [x] 회원 도메인 (가입, 로그인, JWT, Spring Security) + 테스트
- [x] 카테고리 도메인 (2depth 계층 구조, CRUD) + 테스트
- [x] 상품 도메인 (ProductOption 재고, Pessimistic Lock) + 테스트
- [x] 장바구니 도메인 (Cart/CartItem, CRUD, 재고 확인) + 테스트
- [x] 주문 도메인 (Order/OrderItem, CRUD, 재고 차감/복원, 스냅샷) + 테스트
- [x] 결제 도메인 (Payment, Mock PG, 결제 실패 재고 복원, OrderExpirationScheduler) + 테스트
- [x] 멀티모듈 구조 전환 (src/ → apps/api/src/)
- [x] CORS 설정 추가 (localhost:3000, localhost:5173)
- [x] ADMIN 계정 초기화 (DataInitializer: admin@commerce.com / admin1234)
- [x] 상품 이미지 URL 필드 추가 (Product 엔티티 + DTO + 테스트)
- [x] ddl-auto: create-drop → update 변경

## 완료 — 프론트 개발 준비

- [x] 프론트 기술 스택 확정 (Next.js 15 / TypeScript / Tailwind / React Query / Zustand)
- [x] 타입 자동 생성 파이프라인 구성 (openapi-typescript → `npm run generate:types`)
- [x] 프론트 아키텍처 문서 작성 (docs/frontend/)
  - [x] stack.md — 스택 선정 이유, 렌더링 전략
  - [x] architecture.md — 폴더 구조, 라우팅, 상태 관리, API 클라이언트 설계
  - [x] type-generation.md — 타입 생성 파이프라인 가이드

---

## 진행 예정 — 프론트엔드 구축

### 1단계: 프로젝트 초기 설정
- [x] Next.js 앱 생성 (`apps/web/`) — Next.js 16.2.3, Turbopack
- [x] 의존성 설치 (@tanstack/react-query 5, zustand 5, react-query-devtools)
- [x] 환경변수 설정 (`.env.local` — `NEXT_PUBLIC_API_URL=http://localhost:8080`)
- [x] Swagger에서 타입 생성 (`npm run generate:types`) — `api.generated.ts` 생성 완료
- [x] 공통 타입 파일 작성 (`src/types/api.ts` — ApiResponse, PageResponse 래퍼)
- [x] API 클라이언트 작성 (`src/lib/api/client.ts` — apiFetch, ApiError, JWT 자동 첨부)
- [x] React Query QueryClient 설정 및 Provider 연결 (`src/providers/QueryProvider.tsx`)
- [x] Zustand 인증 스토어 작성 (`src/store/auth.ts` — persist + skipHydration)

### 2단계: 레이아웃 및 공통 컴포넌트
- [x] 루트 레이아웃 (`app/layout.tsx`) — Header, Footer 포함
- [x] Header 컴포넌트 — 로고, 카테고리 네비, 장바구니 아이콘, 로그인/로그아웃
- [x] 카테고리 트리 렌더링 (`GET /api/categories`)
- [x] 로그인/회원가입 페이지 (`/signin`, `/signup`)

### 3단계: 상품
- [x] 상품 목록 페이지 (`/`) — SSR, 카테고리 필터, 페이지네이션
- [x] 상품 상세 페이지 (`/products/[id]`) — SSR, 옵션 선택
- [x] 장바구니 담기 버튼 (상품 상세 → `POST /api/cart/items`)
- [x] DataInitializer 샘플 데이터 추가 (카테고리 3개/8개, 상품 14개)

### 4단계: 장바구니 · 주문 · 결제
- [x] 장바구니 페이지 (`/cart`) — 목록, 수량 변경, 삭제, 주문하기
- [x] 주문 생성 (`POST /api/orders`)
- [x] 주문 내역 페이지 (`/orders`)
- [x] 결제 페이지 (`/orders/[id]`) — 결제 수단 선택, `POST /api/payments`
- [x] 주문 취소 (`POST /api/orders/{id}/cancel`)

### 5단계: 관리자 페이지
- [x] ADMIN 전용 레이아웃 — 역할 체크 미들웨어
- [x] 상품 관리 (`/admin/products`) — 목록, 등록, 수정, 삭제
- [x] 카테고리 관리 (`/admin/categories`) — 트리 뷰, 추가, 삭제

---

## 완료 — 백엔드 보완

- [x] 상품 키워드 검색 API (`GET /api/products?keyword=`)
- [x] 프로필 수정 API (`PUT /api/users/me`)
- [x] JWT secret 환경변수 처리 (application.yaml 하드코딩 제거)

---

## 진행 예정 — 무신사 스타일 UI 리디자인

> 레퍼런스: 무신사 — 라이프샷 그리드 피드, 미니멀 헤더, 타이트한 4열 카드

### 6단계: 상품 카드 리디자인 (`ProductCard.tsx`)
- [x] border·shadow·rounded 제거 → 완전 플랫 카드
- [x] 이미지 aspect-square 유지, 호버 시 어두워지는 overlay 효과
- [x] 텍스트: 상품명(sm) + 가격(sm bold) — 패딩 최소화
- [x] 우하단 하트 아이콘 추가

### 7단계: 상품 그리드 조정 (`ProductGrid.tsx`)
- [x] 열 수 조정: `grid-cols-2 sm:grid-cols-3 lg:grid-cols-4` (5열 제거)
- [x] gap 줄임: `gap-4` → `gap-2`
- [x] 빈 상태 문구/아이콘 개선

### 8단계: 헤더 슬림화 (`Header.tsx`)
- [x] 높이 줄임 (py-3 → py-2.5)
- [x] 로고 볼드 + 대문자 스타일 (FASHN)
- [x] 검색: 텍스트+버튼 형태 → 돋보기 아이콘 클릭 시 검색창 expand
- [x] 우측: 텍스트 링크 최소화, 아이콘 위주로 정리

### 9단계: 카테고리 필터 리디자인 (`CategoryFilter.tsx`)
- [x] rounded-full pill → 언더라인 탭 스타일
- [x] 활성 탭: font-bold + border-b-2 border-zinc-900
- [x] 비활성 탭: text-zinc-400, hover 시 text-zinc-900
- [x] 배경 fill 색상 제거

### 10단계: 홈 프로모션 배너 추가 (`page.tsx`)
- [x] 카테고리 필터 위 얇은 배너 띠 (zinc-950 배경 + 흰 텍스트)
- [x] 상품 수 카운터 표시 (무신사 스타일 "전체 상품 (14)")
