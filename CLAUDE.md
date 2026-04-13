# 프로젝트 개요
Kotlin + Spring Boot 4 기반 의류/신발 패션 자사몰 (백엔드 + 프론트엔드 풀스택)

## 기술 스택

### 백엔드 (`apps/api/`)
- Language: Kotlin "2.2.21"
- Framework: Spring Boot "4.0.5"
- ORM: Spring Data JPA + Hibernate
- DB: PostgreSQL
- Build: Gradle (Kotlin DSL)
- Auth: Spring Security + JWT

### 프론트엔드 (`apps/web/`)
- Framework: Next.js 16.2.3 (App Router, Turbopack)
- Language: TypeScript 5
- Styling: Tailwind CSS 4
- 서버 상태: TanStack Query 5
- 클라이언트 상태: Zustand 5
- 타입 생성: openapi-typescript 7

## 주요 명령어

### 백엔드
- `./gradlew bootRun` : 개발 서버 실행
- `./gradlew test` : 전체 테스트 실행
- `./gradlew build` : 빌드
- `./gradlew test --tests "패키지.클래스명"` : 단일 테스트 실행

### 프론트엔드
- `cd apps/web && npm run dev` : 개발 서버 실행 (localhost:3000)
- `cd apps/web && npm run build` : 프로덕션 빌드
- `npm run generate:types` : OpenAPI → TypeScript 타입 생성 (루트에서, 백엔드 기동 필요)

## 도메인 구조
src/main/kotlin/com/example/commerce/
├── user/       # 회원 (가입, 로그인, 프로필)
├── category/   # 카테고리 (2depth 계층 구조)
├── product/    # 상품 (등록, 조회, 옵션별 재고)
├── cart/       # 장바구니
├── order/      # 주문 (생성, 상태 관리)
├── payment/    # 결제 (PG 연동)
└── common/     # 공통 (예외처리, 응답형식, BaseEntity)

## 아키텍처 패턴
- Controller → Service → Repository 레이어드 아키텍처
- Entity와 DTO 엄격히 분리 (data class로 DTO 정의)
- 응답은 ApiResponse<T> 공통 래퍼 사용
- 예외처리: CustomException + @RestControllerAdvice

## 코드 스타일 (IMPORTANT)
- Kotlin nullable 최소화, Elvis 연산자 활용
- data class로 불변 DTO 구성
- companion object에 팩토리 메서드 정의
- JPA Entity는 일반 class 사용 (data class 금지)
- 금액 필드는 반드시 Long 타입 (원 단위 정수)

## 개발 순서 (현재 진행 중)
1. [x] 프로젝트 초기 설정 (공통 모듈, DB 연결)
2. [x] 회원 도메인 (가입, 로그인, JWT)
3. [x] 카테고리 도메인 (2depth 계층 구조)
4. [x] 상품 도메인 (CRUD, ProductOption별 재고, Pessimistic Lock)
5. [x] 장바구니 (Cart/CartItem, CRUD, 재고 확인)
6. [x] 주문 (Order/OrderItem, 재고 차감/복원, 스냅샷)
7. [x] 결제 (PG Mock, 결제 실패 시 재고 복원, 10분 만료 스케줄러)

## Git 워크플로우 (IMPORTANT)
- 기능 구현 완료 시 반드시 커밋까지 진행
- 커밋 전 `./gradlew build` 로 빌드 성공 확인
- 브랜치 전략: feature/기능명 → main PR 머지

## 커밋 메시지 규칙 (Conventional Commits)
- feat: 새 기능
- fix: 버그 수정/
- refactor: 리팩토링
- test: 테스트 추가
- chore: 설정, 의존성 변경

## 커밋 예시
- feat(user): 회원가입 API 구현
- feat(product): 상품 목록 조회 API 구현
- fix(order): 주문 금액 계산 오류 수정

## 커스텀 커맨드

### `/ship`

`.claude/commands/ship.md` 에 정의된 프로젝트 전용 슬래시 커맨드.

테스트 → 커밋 메시지 자동 생성 → 커밋 → 푸시 → 결과 요약을 한 번에 처리한다.

---

## 서비스 규모 및 성능 목표

**예상 사용자**
- DAU: 10,000명
- 피크 동시 접속: ~1,000명
- 피크 RPS: ~100 req/s
- 일 총 요청: ~150만 req/day

**성능 기준 (SLO)**
- API 응답시간: P95 < 200ms
- 가용성: 99.9% (월 다운타임 < 43분)
- 에러율: < 0.1%

**기술 방향**
- DB: HikariCP 커넥션 풀 적절히 설정, 자주 조회되는 컬럼 인덱스 필수
- 캐싱: 상품 목록/상세 등 읽기 빈도 높은 API는 Redis 캐싱 고려
- 인프라: 초기 단일 서버 → 이후 수평 확장(로드밸런서 + 다중 인스턴스) 고려
- 모니터링: 응답시간/에러율/DB 슬로우쿼리 추적 (Spring Actuator + 외부 APM)
- 프론트 배포: CDN 활용, API 서버와 분리 배포

---

## 프로젝트 문서 (docs/)
코드 설계 의도, 아키텍처, 개발 가이드를 담은 문서 모음. API 상세 스펙은 Swagger UI 참조.

| 경로 | 내용 |
|------|------|
| `docs/architecture/` | 시스템 개요, 레이어 아키텍처, JWT 보안 설계 |
| `docs/api/` | API 공통 규칙, 전체 엔드포인트 목록 |
| `docs/database/` | 엔티티 필드 정의, 관계도 |
| `docs/domain/` | 도메인별 비즈니스 규칙 및 유스케이스 |
| `docs/development/` | 로컬 세팅, 코드 컨벤션, 에러 코드 목록 |
| `docs/troubleshooting/` | 개발 중 발생한 문제 상황 및 해결 과정 기록 |

## 도메인별 CLAUDE.md
각 도메인 폴더에 별도 CLAUDE.md를 두어 도메인 특화 규칙을 관리한다.

### 백엔드
| 경로 | 내용 |
|------|------|
| `src/main/kotlin/com/example/commerce/common/CLAUDE.md` | 공통 인프라(BaseEntity, ApiResponse, ErrorCode, CustomException) 사용법 및 주의사항 |
| `src/main/kotlin/com/example/commerce/user/CLAUDE.md` | 회원 도메인 구조, JWT 설계, Spring Security 설정, API 엔드포인트 |
| `src/main/kotlin/com/example/commerce/category/CLAUDE.md` | 카테고리 도메인 구조, 2depth 계층 설계, API 엔드포인트 |
| `src/main/kotlin/com/example/commerce/product/CLAUDE.md` | 상품 도메인 구조, ProductOption 재고 관리, Pessimistic Lock, API 엔드포인트 |
| `src/main/kotlin/com/example/commerce/cart/CLAUDE.md` | 장바구니 도메인 구조, 재고 확인 방식, API 엔드포인트 |
| `src/main/kotlin/com/example/commerce/order/CLAUDE.md` | 주문 도메인 구조, 상태 전이, 재고 처리, 10분 만료 정책, API 엔드포인트 |
| `src/main/kotlin/com/example/commerce/payment/CLAUDE.md` | 결제 도메인 구조, Mock PG 동작, 실패 처리 흐름, API 엔드포인트 |

### 프론트엔드
| 경로 | 내용 |
|------|------|
| `apps/web/CLAUDE.md` | 프론트엔드 아키텍처, 파일 구조, API 클라이언트, 상태 관리, 코드 컨벤션 |