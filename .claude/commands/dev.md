아래 순서를 자동으로 실행하라. **Step 1 완료 후에만 사용자 확인을 요청하고**, 이후 단계는 사용자에게 추가 입력을 요청하지 말고 전체 흐름을 완료하라.

인자가 있으면(예: `/dev notification`) 해당 도메인을 사용한다.
인자가 없으면 사용자에게 도메인명과 기능 설명을 요청하라.

---

## Step 0: feature 브랜치 생성

작업을 시작하기 전에 반드시 `dev` 기준의 feature 브랜치를 생성한다.

```bash
git checkout dev && git pull origin dev
git checkout -b feature/{domain}
```

브랜치 생성 완료 후 "feature/{domain} 브랜치에서 작업을 시작합니다." 출력 후 Step 1 진행.

---

## Step 1: 도메인 계획 수립

사용자로부터 도메인 설명을 받아 아래 설계 명세를 작성하라.
기존 유사 도메인의 코드를 참고해 이 프로젝트 패턴에 맞게 구체화한다.

**출력할 설계 명세 포맷:**

```
## {Domain} 도메인 설계 명세

### Entity
| 필드 | 타입 | nullable | 설명 |
|------|------|----------|------|
| ... | ... | ... | ... |

### Service 메서드
| 메서드명 | 파라미터 | 반환 타입 | 예외 조건 |
|---------|---------|---------|---------|
| ... | ... | ... | ... |

### Controller 엔드포인트
| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| ... | ... | ... | ... |

### ErrorCode
| 상수명 | HTTP | 설명 |
|-------|------|------|
| ... | ... | ... |
```

설계 명세 출력 후 **"위 설계 명세로 진행할까요? 수정이 필요하면 말씀해주세요."** 라고 출력하고 사용자 확인을 기다려라.

---

## Step 2: 테스트 먼저 작성 (TDD)

사용자가 확인하면 즉시 진행한다.

unit-test-generator 에이전트를 **TDD 모드**로 호출한다.
Step 1의 설계 명세 전체를 입력으로 전달하며, 구현 파일은 아직 없으므로 Mode B로 동작하도록 명시한다.

생성 대상:
- `{Domain}ServiceTest.kt` — 각 Service 메서드별 @Nested 구조
- `{Domain}ControllerTest.kt` — 각 엔드포인트별 성공/실패 케이스

테스트 생성 완료 후 "테스트 스켈레톤 생성 완료. 구현 코드를 작성합니다." 출력 후 즉시 다음 단계 진행.

---

## Step 3: 구현 코드 작성

backend-developer 에이전트로 도메인 전체를 구현한다.

에이전트 호출 시 아래를 전달하라:
- Step 1의 설계 명세 전체
- "Step 2에서 생성된 테스트를 통과시키는 것이 목표"
- 프로젝트 패턴: Controller → Service → Repository, CustomException(ErrorCode.XXX), ApiResponse<T> 래퍼

구현 완료 후 즉시 다음 단계 진행.

---

## Step 4: 테스트 통과 확인

```bash
./gradlew test --tests "com.example.commerce.{domain}.*"
```

**실패 시:** 실패한 테스트 목록을 출력하고 backend-developer 에이전트로 수정한다. 전체 통과할 때까지 반복.

**통과 시:** 전체 테스트도 확인한다.

```bash
./gradlew test
```

전체 통과 확인 후 즉시 다음 단계 진행.

---

## Step 5: 구현 커밋

해당 도메인 소스 파일과 테스트 파일만 스테이징한다.

```bash
git add apps/api/src/main/kotlin/com/example/commerce/{domain}/
git add apps/api/src/test/kotlin/com/example/commerce/{domain}/
git add apps/api/src/main/kotlin/com/example/commerce/common/exception/ErrorCode.kt
```

커밋 메시지:
```
feat({domain}): {domain} 도메인 구현

{설계 명세에서 추출한 주요 기능 2-3줄 요약}

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

---

## Step 6: 문서 최신화

`/doc {domain}` 커맨드 흐름을 직접 실행한다 (`.claude/commands/doc.md` 참조).

업데이트 대상:
- `docs/domain/{domain}.md` (신규 생성)
- `docs/api/endpoints.md` (엔드포인트 추가)
- `docs/database/schema.md` (Entity 추가)
- `apps/api/src/main/kotlin/com/example/commerce/{domain}/CLAUDE.md` (신규 생성)
- `docs/development/error-codes.md` (ErrorCode 추가)

---

## Step 7: 문서 커밋 및 푸시

문서 관련 파일만 스테이징한다.

```bash
git add docs/
git add apps/api/src/main/kotlin/com/example/commerce/{domain}/CLAUDE.md
```

커밋 메시지:
```
docs({domain}): {domain} 도메인 문서 추가

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

푸시:
```bash
git push
```

---

## 완료 요약 출력

```
✅ {domain} 도메인 TDD 워크플로우 완료

테스트: N개 통과
구현 커밋: {short-hash} — feat({domain}): ...
문서 커밋: {short-hash} — docs({domain}): ...
푸시 브랜치: {branch}
```
