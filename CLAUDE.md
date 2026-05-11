# Commerce

## 명령어

### 백엔드 (`apps/api/`)
- `./gradlew bootRun` : 개발 서버
- `./gradlew test` : 전체 테스트
- `./gradlew test --tests "패키지.클래스명"` : 단일 테스트
- `./gradlew build` : 빌드

### 프론트엔드 (`apps/web/`)
- `npm run dev` : 개발 서버 (localhost:3000) — **Turbopack 금지**, `next dev --webpack` 사용
- `npm run build` : 프로덕션 빌드
- `npm run lint` : ESLint 검사
- `npm run generate:types` : OpenAPI → TS 타입 생성 (루트에서, 백엔드 기동 필요)

## 코드 규칙

- JPA Entity: 반드시 일반 `class` (data class 금지) + `BaseEntity` 상속
- DTO: 반드시 `data class`, Entity 직접 반환 금지, 응답은 `ApiResponse<T>` 래퍼 필수
- 예외: `throw CustomException(ErrorCode.XXX)`, `ErrorCode` 추가 시 `HttpStatus` + 한국어 메시지 함께
- 금액 필드: 반드시 `Long` (원 단위 정수)
- DB 인덱스: 자주 조회되는 컬럼에 필수 (목표: P95 < 200ms)

## Git 워크플로우

```bash
# 시작
git checkout dev && git pull origin dev && git checkout -b feature/작업명
# 완료
gh pr create --base dev && gh pr merge --merge --delete-branch
```
- 커밋 전 `./gradlew build` 성공 확인
- 커밋 메시지: `feat|fix|refactor|test|chore(scope): 설명`
- `/ship` : 테스트→커밋→푸시 한 번에 (`.claude/commands/ship.md`)
