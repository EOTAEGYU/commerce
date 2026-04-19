# 시스템 아키텍처 개요

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Kotlin 2.2.21 |
| Framework | Spring Boot 4.0.5 |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Build | Gradle (Kotlin DSL) |
| Auth | Spring Security + JWT (JJWT 0.12.6) |
| API 문서 | springdoc-openapi 2.8.8 (Swagger UI) |
| Test | JUnit 5, MockK, H2 |

## 레이어드 아키텍처

```
HTTP Request
     ↓
[Controller]         — 요청/응답 처리, 입력 유효성 검사
     ↓
[Service]            — 비즈니스 로직, 트랜잭션 관리
     ↓
[Repository]         — 데이터 접근 (Spring Data JPA)
     ↓
[Entity / DB]        — PostgreSQL
```

- Controller는 Service만 호출하고, Repository를 직접 호출하지 않는다.
- Service에 `@Transactional` 적용.
- 모든 응답은 `ApiResponse<T>`로 래핑 후 `ResponseEntity`로 반환.

## 패키지 구조

```
com.example.commerce/
├── CommerceApplication.kt
├── common/
│   ├── ApiResponse.kt            # 공통 응답 래퍼
│   ├── BaseEntity.kt             # JPA Audit 베이스 클래스
│   ├── CustomException.kt        # 커스텀 예외
│   ├── ErrorCode.kt              # 에러 코드 열거형
│   ├── GlobalExceptionHandler.kt # 전역 예외 처리
│   ├── config/
│   │   ├── JpaAuditingConfig.kt
│   │   └── SwaggerConfig.kt
│   └── security/
│       ├── JwtProvider.kt
│       ├── JwtAuthenticationFilter.kt
│       └── SecurityConfig.kt
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── product/       # 구현 예정
├── cart/          # 구현 예정
├── order/         # 구현 예정
└── payment/       # 구현 예정
```

## 핵심 설계 원칙

### Entity ↔ DTO 엄격 분리
- Entity는 일반 `class` (data class 금지 — JPA 프록시 이슈)
- DTO는 `data class`로 불변 구성
- Entity를 Controller 응답으로 직접 노출하지 않는다.

### 팩토리 메서드 패턴
```kotlin
// DTO companion object에 from() 정의
data class UserResponse(...) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(...)
    }
}
```

### 금액 필드
- 반드시 `Long` 타입, 원(KRW) 단위 정수로 관리

### Nullable 최소화
- 코틀린 nullable(`?`)은 실제로 null이 가능한 경우에만 사용
- Elvis 연산자(`?:`) 활용
