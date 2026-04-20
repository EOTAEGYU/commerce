프론트엔드 페이지/기능 구현 워크플로우. **Step 1 완료 후에만 사용자 확인을 요청하고**, 이후 단계는 추가 입력 없이 전체 흐름을 완료하라.

인자가 있으면(예: `/front-dev cart`) 해당 기능명을 사용한다.
인자가 없으면 사용자에게 기능명 또는 와이어프레임 파일 경로를 요청하라.

---

## Step 0: feature 브랜치 생성

```bash
git checkout dev && git pull origin dev
git checkout -b feature/{기능명}
```

"feature/{기능명} 브랜치에서 작업을 시작합니다." 출력 후 Step 1 진행.

---

## Step 1: 와이어프레임 분석 + UI 계획 수립

### 1-A. 입력 분류

**와이어프레임 HTML 파일이 제공된 경우:**
- 파일을 읽어 섹션별 레이아웃 variant(A/B/C/D 등)를 파악한다
- 각 variant의 특징을 한 줄로 요약해 선택지로 제시한다

```
[레이아웃 선택]
A — Classic: 테이블 + 우측 sticky Summary (커머스 표준)
B — List: 하단 Summary bar (세로 공간 최대 활용)
C — Grouped: 브랜드/배송 묶음 (멀티 셀러)
D — Empty State: 빈 카트 + 위시리스트 유도

어떤 레이아웃으로 구현할까요?
```

**기능 설명만 제공된 경우:**
- 기존 코드(apps/web/src/app/, apps/web/src/components/)를 탐색해 유사 패턴 파악 후 계획 수립

### 1-B. 계획 명세 출력

아래 포맷으로 구현 계획을 출력한다:

```
## {기능명} UI 구현 계획

### 새로 만들 페이지/컴포넌트
| 경로 | 타입 | 설명 |
|------|------|------|
| app/cart/page.tsx | 'use client' page | ... |
| components/cart/CartSummary.tsx | Client Component | ... |

### 재사용할 기존 컴포넌트
| 컴포넌트 | 경로 | 용도 |
|---------|------|------|
| ProductCard | components/products/ProductCard.tsx | 위시리스트 상품 표시 |

### 연동할 API
| Method | Endpoint | 용도 |
|--------|---------|------|
| GET | /api/cart | 장바구니 조회 |

### 상태 관리
- Server Component / Client Component 분리 기준
- TanStack Query queryKey 목록
- Zustand store 사용 여부
```

계획 출력 후 **"위 계획으로 진행할까요? 레이아웃이나 컴포넌트 구성 변경이 필요하면 말씀해주세요."** 출력 후 사용자 확인 대기.

---

## Step 2: 구현

frontend-developer 에이전트를 호출한다.

에이전트 호출 시 아래를 전달하라:
- Step 1의 구현 계획 전체 (선택된 레이아웃 variant 포함)
- 와이어프레임 파일 경로 (있는 경우)
- 프로젝트 스택: Next.js 16 App Router / TypeScript / Tailwind CSS 4 / TanStack Query 5 / Zustand 5
- API 클라이언트 규칙:
  - 클라이언트 컴포넌트: `apiFetch` (src/lib/api/client.ts) — JWT 자동 첨부
  - 서버 컴포넌트: `serverFetch` (src/lib/api/server.ts) — 공개 API 전용
- 인증 상태: `useAuthStore` (src/store/auth.ts)
- 금액 표시: `toLocaleString('ko-KR')` 원 단위 정수

구현 완료 후 즉시 Step 3 진행.

---

## Step 3: 타입 체크 + 빌드 확인

```bash
cd apps/web && npx tsc --noEmit 2>&1 | tail -20
```

**타입 에러 발생 시:** 에러 내용을 frontend-developer 에이전트에 전달해 수정. 통과할 때까지 반복.

타입 체크 통과 후 빌드 확인:

```bash
cd apps/web && npm run build 2>&1 | tail -30
```

**빌드 실패 시:** 에러 내용을 frontend-developer 에이전트에 전달해 수정. 통과할 때까지 반복.

전체 통과 확인 후 즉시 Step 4 진행.

---

## Step 4: 커밋

변경된 프론트엔드 파일만 스테이징한다.

```bash
git add apps/web/src/
```

커밋 메시지 포맷:
```
feat({기능명}): {기능명} 페이지/컴포넌트 구현

- {구현한 주요 페이지/컴포넌트 2-3줄}
- 와이어프레임 {variant} 레이아웃 적용 (있는 경우)

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

---

## Step 5: 푸시 + PR 생성 → dev merge

```bash
git push -u origin HEAD
```

PR 생성:
```bash
gh pr create --base dev \
  --title "feat({기능명}): {기능명} 페이지/컴포넌트 구현" \
  --body "$(cat <<'EOF'
## Summary
- {구현 내용 bullet 2-3개}
- 와이어프레임 variant {선택 레이아웃} 적용

## 구현 목록
{Step 1에서 정의한 페이지/컴포넌트 목록}

## Test plan
- [ ] TypeScript 타입 체크 통과
- [ ] npm run build 성공
- [ ] 주요 플로우 브라우저 확인

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

PR merge:
```bash
gh pr merge --merge --delete-branch
```

---

## 완료 요약 출력

```
✅ {기능명} 프론트엔드 구현 완료

구현: {페이지/컴포넌트 수}개 파일
레이아웃: variant {선택 레이아웃}
커밋: {short-hash} — feat({기능명}): ...
PR: {PR URL} → dev merge 완료
```
