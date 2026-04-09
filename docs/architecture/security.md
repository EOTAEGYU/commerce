# 보안 설계

## JWT 인증 흐름

```
클라이언트                          서버
   |                                 |
   |── POST /api/users/signin ──────>|
   |   { email, password }           |
   |                                 | 1. 이메일/비밀번호 검증
   |                                 | 2. BCrypt 비교
   |                                 | 3. JWT 생성 (JwtProvider)
   |<── { accessToken: "..." } ──────|
   |                                 |
   |── GET /api/users/me ───────────>|
   |   Authorization: Bearer <token> |
   |                                 | 4. JwtAuthenticationFilter 실행
   |                                 | 5. 토큰 파싱 → userId, email, role 추출
   |                                 | 6. SecurityContext에 인증 정보 저장
   |                                 | 7. Controller에서 @AuthenticationPrincipal로 userId 주입
   |<── { data: { ... } } ───────────|
```

## JWT 토큰 구성

| 클레임 | 값 | 설명 |
|--------|-----|------|
| `userId` | Long | 사용자 ID |
| `email` | String | 이메일 |
| `role` | String | `USER` 또는 `ADMIN` |
| 만료 | 86400000ms | 24시간 |

- 알고리즘: HMAC-SHA (JJWT 기본값)
- 시크릿 키: `application.yaml`의 `jwt.secret`

## 엔드포인트 접근 제어

| 경로 | 인증 필요 |
|------|----------|
| `POST /api/users/signup` | 불필요 |
| `POST /api/users/signin` | 불필요 |
| `/swagger-ui/**` | 불필요 |
| `/v3/api-docs/**` | 불필요 |
| 그 외 모든 경로 | **필요** |

## Spring Security 설정 포인트

- CSRF 비활성화 (JWT 기반 Stateless API)
- Session 정책: `STATELESS`
- `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 등록
- 인증 실패 시 401 반환 (`authenticationEntryPoint`)

## 컴포넌트 역할

| 클래스 | 경로 | 역할 |
|--------|------|------|
| `JwtProvider` | `common/security/` | 토큰 생성, 파싱, 유효성 검증 |
| `JwtAuthenticationFilter` | `common/security/` | 요청마다 Bearer 토큰 추출 및 SecurityContext 설정 |
| `SecurityConfig` | `common/security/` | SecurityFilterChain 빈 정의 |

## 비밀번호 처리

- 저장: `BCryptPasswordEncoder`로 해시화
- 검증: `matches(rawPassword, encodedPassword)` 사용
- 평문 비밀번호는 DB에 저장되지 않는다.
