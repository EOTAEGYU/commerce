# TODO — 하네스 엔지니어링 개선 (2026-04-18)

포트폴리오 보고서에서 도출한 단점/한계 항목을 수정한다.
우선순위 순으로 나열. 하나씩 완료 후 체크.

---

## ✅ 1. Hook 실행 오버헤드 개선 (높음)
> 파일 저장마다 compileKotlin이 동기 실행되어 연속 편집 시 블로킹 발생

- [x] Kotlin 컴파일 hook → `asyncRewake: true` 적용 (백그라운드 실행, 에러 시만 Claude 재기동)
- [x] TypeScript 타입 체크 hook → `asyncRewake: true` 적용
- [x] 컴파일 성공 → exit 0 (조용히 종료) / 실패 → exit 2 (Claude 깨워서 에러 전달)

---

## ✅ 2. 팀 환경 이식성 분리 (높음)
> 현재 모든 설정이 settings.local.json(gitignore)에 있어 팀원과 공유 불가

- [x] `.claude/settings.json` 생성 (팀 공유용 — 커밋 대상)
  - 공유할 항목: permissions.allow (gradlew, git 명령어)
  - 공유할 항목: enabledMcpjsonServers
- [x] settings.local.json에는 개인 항목만 남기기
  - 개인 항목: Stop hook (Windows 알림 — OS 종속)
  - 개인 항목: PostToolUse hooks (로컬 환경 경로 의존)

---

## ✅ 3. 에이전트 트리거 정확도 최적화 (중간)
> Description이 모호하면 잘못된 에이전트가 선택됨

- [x] `backend-developer` description에 트리거 예시 보강 (프론트 요청과 혼동 방지)
- [x] `frontend-developer` description에 명시적 트리거 키워드 추가
  - "Next.js", "React", "컴포넌트", "페이지", "프론트"
- [x] `unit-test-generator` description에 파일 경로 패턴 명시 + 경로 없이도 트리거 가능하도록 개선
- [ ] skill-creator의 description optimizer로 각 에이전트 description 점수 측정

---

## ✅ 4. Agent Memory 진부화 방지 (중간)
> 누적된 메모리가 코드 변화와 어긋날 경우 잘못된 가이드가 될 수 있음

- [x] SessionStart hook 추가: 세션 시작 시 agent-memory 파일의 최종 수정일 출력 (30일 초과 시 ⚠️ 경고)
- [x] unit-test-generator memory 내용 검토 — 현재 코드와 일치 확인 (수정 불필요)
- [x] backend-developer memory 폴더에 MEMORY.md 인덱스 생성
- [x] 메모리 감사 주기 정책: 도메인 5개 추가마다 1회, 또는 메모리 파일 수정일 30일 초과 시 자동 경고

---

## ✅ 5. 컨텍스트 창 소비 최적화 (낮음)
> 작업과 무관한 도메인 CLAUDE.md도 전부 로드되어 컨텍스트 낭비

- [x] 현재 로드되는 CLAUDE.md 목록 및 토큰 크기 측정 (총 ~9,900 토큰, 200k 컨텍스트의 5%)
- [x] `claudeMdExcludes` 패턴 활용 검토 → 해당 기능 Claude Code에 존재하지 않음 (미지원)
- [x] 도메인 CLAUDE.md 내용 중 중복 규칙 제거:
  - 루트 CLAUDE.md `Turbopack` → `webpack` 오류 수정
  - 루트 CLAUDE.md `## 개발 순서` 제거 (~150 토큰 절감, 이미 완료된 도메인 목록)
  - apps/web/CLAUDE.md `## 기술 스택` 제거 (~80 토큰 절감, 루트에 이미 있음)
  - 최종 절감: 928자 → 약 230 토큰 절약

---

## ✅ 6. CLAUDE.md ↔ 코드 동기화 자동화 (낮음)
> 비즈니스 규칙 변경 시 코드와 CLAUDE.md를 수동으로 동시 업데이트해야 함

- [x] `/doc` 커맨드에 도메인 CLAUDE.md 생성 기능 추가
  - 신규 도메인 추가 시 git diff 분석 → Entity/비즈니스규칙/API/ErrorCode 자동 추출해 생성
- [x] `/doc` 커맨드에 ErrorCode 변경 감지 → `docs/development/error-codes.md` 자동 반영
  - ErrorCode.kt diff에서 새 enum 상수 추출 → 도메인 섹션 테이블에 행 자동 추가

---

## ✅ 완료된 하네스 구성 (참고)

- [x] 루트 CLAUDE.md + 도메인별 CLAUDE.md 12개
- [x] Sub-agent 3개 (backend-developer, unit-test-generator, frontend-developer)
- [x] PostToolUse hook — Kotlin 컴파일 자동 체크
- [x] PostToolUse hook — TypeScript 타입 자동 체크
- [x] Stop hook — Windows 완료 알림
- [x] /ship 커맨드 (테스트 → 커밋 → 푸시 자동화)
- [x] /doc 커맨드 (git diff → 문서 자동 업데이트)
- [x] plan-review MCP (Gemini 이중 검증)
- [x] Permissions 10개 사전 허용
- [x] Agent Memory (unit-test-generator 패턴 누적)

---

# TODO — 와이어프레임 구현 (2026-04-20)

## ✅ 01. Discovery — Home / Category / Search / PDP
- [x] Home 메인 페이지 (HeroBanner, New Arrivals, Best Sellers, Category 배너)
- [x] Category 목록 페이지 (좌측 필터 사이드바 + 4열 그리드)
- [x] Search 결과 페이지 (필터 사이드바 + 4열 그리드)
- [x] PDP 상품 상세 페이지 (갤러리 그리드 + sticky 패널)

---

## 02. Cart — Checkout — Payment
- [ ] 장바구니 페이지 (Classic 레이아웃: 테이블 + 우측 sticky Summary)
- [ ] 체크아웃 페이지 (배송지 입력, 쿠폰/포인트 선택, 결제수단)
- [ ] 결제 진행 / 완료 / 실패 상태 페이지

---

## 03. Auth — MyPage
- [ ] 로그인 / 회원가입 페이지
- [ ] 마이페이지 홈 대시보드 (통계카드, 진행중 주문, 리뷰 대기)
- [ ] 주문 목록 / 주문 상세 페이지 (배송 타임라인, 결제내역)
- [ ] 리뷰 목록 / 리뷰 작성 화면
- [ ] 위시리스트 페이지 (그리드 레이아웃)
- [ ] 쿠폰함 / 포인트 이력 페이지
