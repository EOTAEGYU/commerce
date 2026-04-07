# user 도메인

회원 가입, 로그인, JWT 인증, 프로필 조회를 담당한다.

## 폴더 구조
```
user/
├── controller/   # UserController
├── service/      # UserService
├── repository/   # UserRepository
├── entity/       # User, UserRole
└── dto/          # SignUpRequest, SignInRequest, AuthResponse, UserResponse
```

## Entity
- `User` : email, password(BCrypt), name, role(UserRole)
- `UserRole` : enum — `USER`, `ADMIN`
- BaseEntity 상속 필수

## 주요 ErrorCode (ErrorCode.kt에 추가)
- `DUPLICATE_EMAIL` — 이미 사용 중인 이메일
- `USER_NOT_FOUND` — 존재하지 않는 회원
- `INVALID_CREDENTIALS` — 이메일 또는 비밀번호 불일치

## API 엔드포인트
| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | /api/users/signup | 불필요 | 회원가입 |
| POST | /api/users/signin | 불필요 | 로그인 → JWT 반환 |
| GET  | /api/users/me | 필요 | 내 프로필 조회 |

## JWT
- Access Token 클레임: `userId`, `email`, `role`
- 토큰 생성/검증: `JwtProvider` (common/security 또는 user/util에 위치)
- Spring Security Filter에서 토큰 검증 후 SecurityContext에 인증 정보 저장

## Spring Security
- 공개 경로: `/api/users/signup`, `/api/users/signin`
- 나머지 경로: 인증 필요
- `SecurityConfig`는 `SecurityFilterChain` 빈으로 구성 (WebSecurityConfigurerAdapter 사용 금지)
