# Claude Code 하네스 엔지니어링

이 프로젝트는 Claude Code를 중심으로 하네스(harness)를 구성해 AI 개발 지원 품질을 높인다.
하네스란 Claude가 더 정확하고 일관되게 작동하도록 설정, 에이전트, 훅, 메모리, MCP를 조합한 구조를 말한다.

---

## 구성 요소 전체 목록

| 구성 요소 | 파일 위치 | 역할 |
|----------|-----------|------|
| 루트 컨텍스트 | `CLAUDE.md` | 프로젝트 개요, 아키텍처 패턴, 코드 스타일, Git 규칙 |
| 도메인 컨텍스트 | `apps/api/src/.../도메인/CLAUDE.md` × 11 | 도메인별 Entity, 비즈니스 규칙, ErrorCode |
| 프론트 컨텍스트 | `apps/web/CLAUDE.md` | Next.js 파일 구조, API 클라이언트, 상태 관리 패턴 |
| Sub-agent × 3 | `.claude/agents/` | 역할별 전문 AI 에이전트 |
| 커스텀 커맨드 × 2 | `.claude/commands/` | 반복 워크플로우 자동화 |
| PostToolUse Hook × 2 | `.claude/settings.json` | 파일 저장 시 자동 컴파일 체크 |
| SessionStart Hook | `.claude/settings.local.json` | 세션 시작 시 메모리 진부화 경고 |
| Stop Hook | `.claude/settings.local.json` | 작업 완료 시 Windows 알림 |
| Agent Memory | `.claude/agent-memory/` | 에이전트 패턴 누적 학습 |
| MCP 서버 | `tools/plan-review/` | Gemini 기반 플랜 이중 검증 |

---

## 1. CLAUDE.md — 컨텍스트 계층

Claude Code는 현재 작업 디렉토리에서 루트 방향으로 CLAUDE.md를 자동 로드한다.
이 프로젝트는 2단계 계층 구조를 사용한다.

```
CLAUDE.md                          ← 항상 로드 (1,665 토큰)
├── apps/web/CLAUDE.md             ← 프론트 작업 시만 로드
└── apps/api/.../도메인/CLAUDE.md  ← 해당 도메인 작업 시만 로드
```

### 루트 CLAUDE.md 주요 내용
- 기술 스택 (Kotlin/Spring Boot 4 + Next.js 16)
- 레이어드 아키텍처 패턴 (Controller → Service → Repository)
- 코드 스타일 규칙 (Kotlin nullable, Long 금액, data class DTO 등)
- Git 워크플로우 및 Conventional Commits 규칙

### 도메인 CLAUDE.md 포맷 (11개 도메인)
각 도메인 CLAUDE.md는 `/doc` 커맨드로 자동 생성·갱신된다.

```markdown
# {Domain} 도메인
{한 문장 기능 설명}

## 폴더 구조    — 실제 파일 트리
## Entity       — 주요 필드 및 타입
## 비즈니스 규칙 — 서비스 메서드별 흐름, 예외 조건
## API 엔드포인트 — Method/Path/Auth/설명
## 주요 ErrorCode — 코드/HTTP/메시지
## 주의사항      — Lock 방식, Self-invocation 등 특이 패턴
```

**총 컨텍스트 크기:** 전체 CLAUDE.md 합계 약 9,700 토큰 (200k 컨텍스트의 ~5%)

---

## 2. Sub-agent — 역할별 전문 에이전트

`.claude/agents/` 에 정의된 에이전트는 Claude가 자연어 요청을 받으면 description을 보고 자동 선택한다.

| 에이전트 | 색상 | 트리거 조건 | 담당 |
|---------|------|------------|------|
| `backend-developer` | 🔵 파랑 | 도메인/API/Entity/Service/JPA/백엔드 키워드 | Kotlin + Spring Boot 4 전 계층 구현 |
| `frontend-developer` | 🟣 보라 | 페이지/컴포넌트/UI/화면/Next.js/프론트 키워드 | Next.js 16 App Router + TanStack Query + Zustand |
| `unit-test-generator` | 🟢 초록 | 테스트/test/유닛 테스트 키워드 (경로 없어도 가능) | 레이어별 테스트 자동 생성 (MockK/WebMvcTest/DataJpaTest) |

각 에이전트의 description에는 **트리거 키워드**와 **혼동 방지 반례**가 명시되어 있어 선택 정확도를 높인다.

```
"OrderService 테스트 만들어줘"  → unit-test-generator (경로 없어도 선택됨)
"포인트 도메인 만들어줘"        → backend-developer
"쿠폰 내역 페이지 만들어줘"     → frontend-developer
```

---

## 3. 커스텀 커맨드

`.claude/commands/` 에 정의된 슬래시 커맨드.

### `/ship` — 배포 파이프라인 자동화
테스트 → 커밋 메시지 자동 생성 → 커밋 → 푸시 → 결과 요약을 한 번에 처리한다.

```
/ship
  ↓ ./gradlew test (실패 시 중단)
  ↓ git diff 분석 → Conventional Commits 메시지 생성
  ↓ git add + commit + push
  ↓ 완료 요약 출력
```

### `/doc` — 문서 자동 갱신
`git diff main..HEAD` 를 분석해 변경된 도메인의 문서를 자동 업데이트한다.

```
/doc [도메인명]
  ↓ 변경된 파일에서 도메인 추론
  ↓ docs/domain/{domain}.md 업데이트
  ↓ docs/api/endpoints.md 업데이트
  ↓ docs/database/schema.md 업데이트 (Entity 변경 시)
  ↓ apps/api/.../CLAUDE.md 자동 생성 (신규 도메인)
  ↓ docs/development/error-codes.md 갱신 (ErrorCode 추가 시)
```

---

## 4. Hooks — 자동 품질 체크

### settings.json (팀 공유, 커밋 대상)

**PostToolUse — Kotlin 컴파일 체크**
`.kt` 파일이 저장될 때마다 백그라운드에서 `./gradlew :apps:api:compileKotlin` 실행.
성공하면 조용히 종료, 실패하면 에러를 Claude에게 전달해 즉시 수정 지시.

**PostToolUse — TypeScript 타입 체크**
`.ts/.tsx` 파일이 저장될 때마다 `npx --prefix apps/web tsc --noEmit` 실행.
타입 에러 발생 시 Claude 재기동.

```json
{
  "asyncRewake": true,
  "rewakeSummary": "컴파일 에러 발생",
  "rewakeMessage": "에러를 확인하고 즉시 수정하라:"
}
```

`asyncRewake: true` 덕분에 훅이 백그라운드에서 실행되어 연속 편집 시 블로킹이 없다.
에러가 있을 때만 Claude를 깨운다 (exit 0 = 조용히 종료, exit 2 = 재기동).

### settings.local.json (개인용, gitignore)

**SessionStart — 메모리 진부화 경고**
세션 시작 시 agent-memory 파일의 수정일을 Claude 컨텍스트에 주입.
30일 이상 경과한 파일은 `⚠️ 검증 권장` 경고 포함.

**Stop — Windows 완료 알림**
Claude 작업 완료 시 Windows 풍선 알림 표시 (OS 종속이므로 개인 설정).

### settings.json vs settings.local.json 분리 원칙

| 항목 | 파일 | 이유 |
|------|------|------|
| permissions.allow (gradlew, git) | `settings.json` | 팀 전체 공유 |
| enabledMcpjsonServers | `settings.json` | 팀 전체 공유 |
| PostToolUse 컴파일 훅 | `settings.json` | 팀 전체 공유 |
| Stop 알림 훅 | `settings.local.json` | Windows OS 종속 |
| SessionStart 메모리 훅 | `settings.local.json` | 로컬 경로 의존 |

---

## 5. Agent Memory — 에이전트 학습 누적

`.claude/agent-memory/{에이전트명}/` 디렉토리에 대화 간 학습 내용을 저장한다.
각 에이전트는 `MEMORY.md` 인덱스 파일과 개별 `.md` 메모리 파일로 구성된다.

```
.claude/agent-memory/
├── unit-test-generator/
│   ├── MEMORY.md
│   ├── feedback_controller_test_patterns.md  ← Spring Boot 4 import, csrf, Jackson 3.x
│   └── feedback_service_test_patterns.md     ← MockK 패턴, factory 방식, 경계 케이스
├── backend-developer/
│   └── MEMORY.md
└── frontend-developer/
    └── (신규 학습 시 추가)
```

**메모리 진부화 방지 정책:**
- SessionStart 훅이 세션마다 파일 수정일을 Claude에게 알림
- 30일 이상 경과 시 코드와 대조해 검증 권장
- 도메인 5개 추가마다 1회 전체 감사

---

## 6. MCP 서버 — 플랜 이중 검증

`tools/plan-review/` 에 Gemini API 기반 MCP 서버가 있다.
복잡한 구현 계획을 수립할 때 Claude 플랜을 Gemini에게 검토시켜 이중 검증한다.

```
Claude 플랜 작성 → plan-review MCP → Gemini 분석 → 보완점 피드백 → 구현 시작
```

설정 위치: `.mcp.json` (GEMINI_API_KEY 환경변수 필요)

---

## 7. Permissions — 사전 허용 목록

매번 승인 프롬프트 없이 실행되는 명령어 목록 (`settings.json`):

```
Bash(./gradlew build *)
Bash(./gradlew :apps:api:compileTestKotlin)
Bash(./gradlew test *)
Bash(./gradlew bootRun)
Bash(git diff *)
Bash(git log *)
Bash(git status)
Bash(git add *)
Bash(git commit *)
Bash(git push *)
```

---

## 하네스 설계 원칙

1. **컨텍스트는 계층적으로** — 루트는 전체 개요, 도메인 CLAUDE.md는 해당 작업 시만 로드
2. **훅은 비동기로** — `asyncRewake: true`로 블로킹 없이 에러만 감지
3. **팀 공유 vs 개인 분리** — `settings.json` (커밋) / `settings.local.json` (gitignore)
4. **에이전트 선택은 키워드로** — description에 트리거 키워드 + 반례 명시
5. **메모리는 주기적으로 검증** — 코드 변화에 따라 30일 주기로 정확성 체크
6. **문서는 자동으로** — `/doc` 커맨드로 코드 변경 시 CLAUDE.md와 docs/ 동시 갱신
