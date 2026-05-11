# 회원 도메인 (User)

## 개요

회원 가입, 로그인, 프로필 조회를 담당합니다. JWT 기반 인증의 시작점입니다.

## 비즈니스 규칙

- 이메일, 아이디(username), 전화번호는 유일해야 한다 (중복 불가).
- 아이디는 영문 소문자와 숫자만 4~16자로 구성한다.
- 비밀번호는 최소 8자 이상이어야 한다.
- 비밀번호는 BCrypt로 해시화하여 저장하고, 평문은 저장하지 않는다.
- 회원 역할(role)은 `USER` 또는 `ADMIN`이며, 기본값은 `USER`.
- 로그인 성공 시 JWT Access Token(24시간 유효)을 발급한다.
- 소셜 로그인(OAuth2) 계정은 비밀번호가 없으므로 일반 로그인 불가.

## 주요 유스케이스

### 1. 아이디 중복 확인 (checkUsernameDuplicate)
1. `username` 쿼리 파라미터로 중복 여부 조회
2. `{ available: true/false }` 반환

### 2. 회원가입 (signUp)
1. 이메일 중복 확인 → 중복이면 `DUPLICATE_EMAIL` 예외
2. 아이디 중복 확인 → 중복이면 `DUPLICATE_USERNAME` 예외
3. 전화번호 중복 확인 → 중복이면 `DUPLICATE_PHONE` 예외
4. 비밀번호 BCrypt 해시화
5. User 엔티티 저장
6. `UserResponse` 반환

### 3. 로그인 (signIn)
1. **아이디(username)**로 회원 조회 → 없으면 `INVALID_CREDENTIALS` 예외
2. OAuth2 계정(password=null)이면 `INVALID_CREDENTIALS` 예외
3. 비밀번호 BCrypt 검증 → 불일치 시 `INVALID_CREDENTIALS` 예외
4. JWT 생성 (userId, email, role 포함)
5. `AuthResponse(accessToken)` 반환

### 4. 내 프로필 조회 (getMe)
1. SecurityContext에서 추출된 userId로 회원 조회
2. 없으면 `USER_NOT_FOUND` 예외
3. `UserResponse` 반환

## DTO 명세

### SignUpRequest
| 필드 | 타입 | 제약 |
|------|------|------|
| email | String | 필수, 이메일 형식 |
| password | String | 필수, 8자 이상 |
| name | String | 필수 |
| username | String | 필수, 영문 소문자·숫자 4~16자 |
| phoneNumber | String | 필수, 01x-xxxx-xxxx 형식 (하이픈 생략 가능) |
| birthDate | LocalDate | 필수, 과거 날짜만 허용 |

### SignInRequest
| 필드 | 타입 | 제약 |
|------|------|------|
| username | String | 필수 |
| password | String | 필수 |

### AuthResponse
| 필드 | 타입 | 설명 |
|------|------|------|
| accessToken | String | JWT 토큰 |
| tokenType | String | "Bearer" (고정) |

### UserResponse
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 회원 ID |
| email | String | 이메일 |
| name | String | 이름 |
| role | UserRole | USER / ADMIN |
| username | String? | 아이디 (소셜 전용 계정은 null) |
| phoneNumber | String? | 전화번호 |
| birthDate | LocalDate? | 생년월일 |

## 에러 코드

| 코드 | HTTP | 상황 |
|------|------|------|
| `DUPLICATE_EMAIL` | 409 | 이미 사용 중인 이메일로 회원가입 시도 |
| `DUPLICATE_USERNAME` | 409 | 이미 사용 중인 아이디로 회원가입 시도 |
| `DUPLICATE_PHONE` | 409 | 이미 사용 중인 전화번호로 회원가입 시도 |
| `USER_NOT_FOUND` | 404 | 존재하지 않는 userId로 조회 |
| `INVALID_CREDENTIALS` | 401 | 아이디 없거나 비밀번호 불일치 |
