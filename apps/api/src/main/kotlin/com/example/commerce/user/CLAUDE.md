# user 도메인

회원 가입, 로그인(아이디 기반), JWT 인증, 프로필 조회를 담당한다.

## 폴더 구조
```
user/
├── controller/   # UserController
├── service/      # UserService
├── repository/   # UserRepository, SocialAccountRepository
├── entity/       # User, UserRole, SocialAccount
└── dto/          # SignUpRequest, SignInRequest, AuthResponse, UserResponse, UpdateProfileRequest
```

## Entity
- `User` : email, password(BCrypt), name, role(UserRole), username, phoneNumber, birthDate
  - username/phoneNumber/birthDate 는 nullable (소셜 전용 계정은 없을 수 있음)
  - email/username/phoneNumber 는 UNIQUE 제약
- `UserRole` : enum — `USER`, `ADMIN`
- BaseEntity 상속 필수

## 비즈니스 규칙
- 로그인은 **아이디(username) + 비밀번호** 방식 (이메일 로그인 아님)
- 소셜 로그인(OAuth2) 계정은 password=null → 일반 로그인 시 `INVALID_CREDENTIALS` 반환
- 회원가입 시 이메일/아이디/전화번호 중복을 각각 별도 예외로 구분
- JWT 토큰 클레임은 userId 기반이므로 `JwtAuthenticationFilter` 변경 불필요

## 주요 ErrorCode (ErrorCode.kt에 추가)
- `DUPLICATE_EMAIL` — 이미 사용 중인 이메일
- `DUPLICATE_USERNAME` — 이미 사용 중인 아이디
- `DUPLICATE_PHONE` — 이미 사용 중인 전화번호
- `USER_NOT_FOUND` — 존재하지 않는 회원
- `INVALID_CREDENTIALS` — 아이디 없거나 비밀번호 불일치

## API 엔드포인트
| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| GET  | /api/users/check/username?value= | 불필요 | 아이디 중복 확인 |
| POST | /api/users/signup | 불필요 | 회원가입 |
| POST | /api/users/signin | 불필요 | 로그인 → JWT 반환 |
| GET  | /api/users/me | 필요 | 내 프로필 조회 |
| PUT  | /api/users/me | 필요 | 내 프로필 수정 |

## JWT
- Access Token 클레임: `userId`, `email`, `role`
- 토큰 생성/검증: `JwtProvider` (common/security에 위치)
- Spring Security Filter에서 토큰 검증 후 SecurityContext에 인증 정보 저장

## Spring Security
- 공개 경로: `/api/users/signup`, `/api/users/signin`, `/api/users/check/username`
- 나머지 경로: 인증 필요
- `SecurityConfig`는 `SecurityFilterChain` 빈으로 구성 (WebSecurityConfigurerAdapter 사용 금지)
