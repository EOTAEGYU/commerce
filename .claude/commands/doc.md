아래 순서를 자동으로 실행하라. 사용자에게 추가 입력을 요청하지 말고 전체 흐름을 완료하라.

## Step 1: 변경 대상 도메인 파악

인자가 있으면(예: `/doc review`) 해당 도메인을 사용한다.
인자가 없으면 `git diff main..HEAD --name-only` 로 변경된 파일 목록을 확인하고, 경로에서 도메인을 추론한다.

```
apps/api/src/main/kotlin/com/example/commerce/{domain}/... → {domain}
```

추론한 도메인 목록을 출력하고 즉시 다음 단계로 진행하라. 확인을 요청하지 않는다.

---

## Step 2: 변경 내용 분석

각 도메인별로 아래를 실행한다.

```bash
git diff main..HEAD -- apps/api/src/main/kotlin/com/example/commerce/{domain}/
```

분석할 항목:
- **새 Entity 필드/클래스** → DB 스키마 문서 업데이트 필요
- **새 API 엔드포인트** (`@GetMapping`, `@PostMapping` 등) → endpoints.md 업데이트 필요
- **비즈니스 규칙 변경** (Service 메서드, `throw CustomException` 패턴) → 도메인 문서 + 도메인 CLAUDE.md 업데이트 필요
- **도메인 CLAUDE.md 존재 여부** → 없으면 Step 3-4에서 자동 생성

ErrorCode.kt 변경을 별도로 확인한다:
```bash
git diff main..HEAD -- apps/api/src/main/kotlin/com/example/commerce/common/exception/ErrorCode.kt
```
`+` 로 시작하는 줄에서 새로 추가된 enum 상수 목록을 추출하고, `// {Domain}` 주석 기준으로 도메인별로 그룹핑한다.

---

## Step 3: 문서 업데이트

변경 내용에 따라 아래 파일들을 업데이트한다. **파일을 수정하기 전에 반드시 먼저 Read한다.**

### 3-1. 도메인 문서 (`docs/domain/{domain}.md`)

- 도메인 파일이 없으면 새로 생성한다 (기존 파일 포맷 참고: `docs/domain/review.md`)
- 비즈니스 규칙이 바뀌었으면 해당 섹션을 업데이트한다
- 새 기능이 추가됐으면 개요와 비즈니스 규칙 섹션에 반영한다

### 3-2. API 엔드포인트 목록 (`docs/api/endpoints.md`)

- 새 Controller 메서드가 추가됐으면 해당 도메인 섹션에 행을 추가한다
- 없던 도메인 섹션이면 기존 포맷에 맞춰 새 섹션을 추가한다
- 메서드, URL, 인증 필요 여부, 설명을 정확히 기재한다

### 3-3. DB 스키마 (`docs/database/schema.md`) — Entity 변경 시만

- 새 Entity가 추가됐거나 필드가 바뀐 경우에만 업데이트한다

### 3-4. 도메인 CLAUDE.md (`apps/api/src/main/kotlin/com/example/commerce/{domain}/CLAUDE.md`)

**존재하는 경우:** 비즈니스 규칙이나 설계 결정이 바뀐 경우 해당 섹션만 수정한다.

**존재하지 않는 경우 (신규 도메인):** git diff를 분석해 아래 포맷으로 자동 생성한다.
파일을 쓰기 전에 git diff에서 실제 내용을 충분히 읽어 각 섹션을 채운다.

```markdown
# {Domain} 도메인

{git diff의 Service/Entity에서 추론한 한 문장 기능 설명}

## 폴더 구조
{git diff --name-only 기반으로 실제 생성된 파일 목록을 tree 형식으로}

## Entity
{git diff에서 class 선언과 주요 필드 추출}

## 비즈니스 규칙
{Service 메서드별 흐름 + throw CustomException 패턴에서 추출한 규칙}

## API 엔드포인트
| Method | Path | 인증 | 설명 |
|--------|------|------|------|
{Controller의 @*Mapping 어노테이션에서 추출}

## 주요 ErrorCode
| 코드 | 상황 |
|------|------|
{ErrorCode.kt에서 해당 도메인 그룹 추출}

## 주의사항
{@Lock, @Version, Optimistic/Pessimistic Lock, self-invocation 등 특이 패턴이 있을 경우만 기재}
```

### 3-5. 에러코드 문서 (`docs/development/error-codes.md`) — ErrorCode.kt 변경 시만

Step 2에서 새로 추가된 enum 상수가 있으면:

1. `docs/development/error-codes.md` Read
2. 해당 도메인 섹션을 찾는다 (없으면 파일 하단에 새 섹션 추가)
3. 새 enum 상수를 아래 포맷으로 테이블 행 변환:
   - **코드**: enum 이름 (예: `POINT_INSUFFICIENT`)
   - **HTTP 상태**: `HttpStatus.XXX` → 숫자 (예: `400`)
   - **메시지**: enum의 message 파라미터 (한국어 그대로)
   - **발생 상황**: 메시지에서 발생 맥락 추론
4. 해당 도메인 테이블에 행 추가 후 Write

---

## Step 4: 완료 요약 출력

업데이트한 파일 목록과 주요 변경 내용을 간결하게 출력한다.

```
업데이트 완료:
- docs/domain/point.md — 포인트 적립/차감 규칙 추가
- docs/api/endpoints.md — POST /api/points/use 엔드포인트 추가
- apps/api/.../point/CLAUDE.md — 신규 생성 (Entity, 비즈니스 규칙, API 자동 추출)
- docs/development/error-codes.md — POINT_INSUFFICIENT 등 4개 에러코드 추가
```
