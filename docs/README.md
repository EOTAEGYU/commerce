# Commerce API 문서

Kotlin + Spring Boot 4 기반 커머스 백엔드 프로젝트 문서 모음입니다.

## 문서 구조

### Architecture
- [시스템 개요](architecture/overview.md) — 기술 스택, 레이어 아키텍처, 패키지 구조
- [보안 설계](architecture/security.md) — JWT 인증 흐름, Spring Security 설정

### API
- [API 공통 규칙](api/conventions.md) — 응답 형식, 인증 방법, 에러 처리
- [엔드포인트 목록](api/endpoints.md) — 전체 API 엔드포인트 요약

### Database
- [스키마 정의](database/schema.md) — 엔티티 필드, 관계도

### Domain
- [회원 (User)](domain/user.md) — 비즈니스 규칙, 유스케이스
- [상품 (Product)](domain/product.md) — 상품 관리, 재고 정책
- [장바구니 (Cart)](domain/cart.md) — 장바구니 관리
- [주문 (Order)](domain/order.md) — 주문 생성, 상태 전이
- [결제 (Payment)](domain/payment.md) — 결제 처리 흐름
- [리뷰 (Review)](domain/review.md) — 리뷰 관리
- [좋아요 (Like)](domain/like.md) — 상품 찜 기능

### Development
- [시작 가이드](development/getting-started.md) — 로컬 환경 세팅, 실행 방법
- [코드 컨벤션](development/conventions.md) — 코딩 스타일, 커밋 규칙
- [에러 코드 목록](development/error-codes.md) — 전체 ErrorCode 정의

### Frontend
- [기술 스택](frontend/stack.md) — Next.js, TypeScript, Tailwind, React Query, Zustand
- [아키텍처](frontend/architecture.md) — 폴더 구조, 라우팅 전략, 상태 관리, API 클라이언트
- [타입 자동 생성](frontend/type-generation.md) — Swagger → TypeScript 타입 생성 파이프라인

## 빠른 참조

| 항목 | 위치 |
|------|------|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| API Docs JSON | http://localhost:8080/v3/api-docs |
| 서버 포트 | 8080 |
| DB | PostgreSQL localhost:5432/commerce |
