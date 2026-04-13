# Turbopack Windows 충돌 (os error 1450 + tailwindcss 해석 실패)

## 문제 상황

`npm run dev` 실행 후 브라우저에서 아래 에러 발생:

```
Runtime Error
An unexpected Turbopack error occurred.
Please see the output of `next dev` for more details.
```

서버 로그에는 두 가지 에러가 함께 출력됨:

```
Error: Can't resolve 'tailwindcss' in 'C:\...\commerce\apps'

FATAL: An unexpected Turbopack error occurred.
- failed to write to "...\apps\web\.next\dev\build\postcss.js"
- 시스템 리소스가 부족하기 때문에 요청한 서비스를 완성할 수 없습니다. (os error 1450)
```

## 원인 분석

### 1. `os error 1450` — Windows 커널 리소스 부족
- Turbopack은 webpack보다 훨씬 많은 파일을 동시에 감시(watch)함
- Spring Boot(Gradle) + Turbopack Dev 서버를 동시에 실행하면 Windows 커널의 파일 핸들이 고갈됨
- Windows error 1450: `ERROR_NO_SYSTEM_RESOURCES` (paged pool 또는 파일 핸들 한계 초과)
- Turbopack이 `.next/dev/build/postcss.js`에 쓰기를 시도하는 순간 실패 → 패닉

### 2. `Can't resolve 'tailwindcss'` — 모듈 경로 오해석
- Turbopack의 PostCSS 워커(`evaluate_webpack_loader`)가 패닉 상태에서 모듈 해석 컨텍스트를 `apps/web/` 대신 `apps/`로 잘못 설정함
- `tailwindcss`는 `apps/web/node_modules/`에만 설치되어 있으므로 해석 실패
- 루트 `package.json`이 존재해 enhanced-resolve가 잘못된 패키지 경계를 인식한 것도 원인

## 해결 과정

1. 실행 중인 Node 프로세스 종료 및 `.next` 캐시 삭제
2. `package.json` dev 스크립트 변경:
   ```json
   // 변경 전
   "dev": "next dev"

   // 변경 후 (Next.js 16 기준)
   "dev": "next dev --webpack"
   ```
3. Next.js 16의 정확한 플래그 확인 (`next dev --help` → `--webpack` 옵션 존재)
4. webpack 모드로 재시작

## 결과

webpack 모드에서 정상 기동 (`✓ Ready in Nms`).

Turbopack은 Windows 환경에서 Spring Boot와 동시 실행 시 리소스 충돌이 발생하므로, 안정화 전까지 webpack 모드 사용.

## 참고

- Next.js 16 CLI 옵션: `next dev --webpack` (webpack), `next dev --turbopack` (Turbopack 명시)
- Turbopack은 Linux/macOS 환경에서 더 안정적으로 동작함
- Windows에서 개선이 필요한 경우 Node.js 파일 감시 수를 제한하는 `watchOptions` 설정 고려
