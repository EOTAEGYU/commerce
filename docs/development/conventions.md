# 코드 컨벤션 & 개발 규칙

## 코딩 스타일

### Entity
```kotlin
// Entity는 일반 class (data class 금지 — JPA 프록시 이슈)
@Entity
@Table(name = "users")
class User(
    @Column(nullable = false, unique = true)
    var email: String,
    ...
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
```

### DTO
```kotlin
// DTO는 data class + companion object 팩토리 메서드
data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: UserRole,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role,
        )
    }
}
```

### Controller
```kotlin
// 반환 타입: ResponseEntity<ApiResponse<T>>
@PostMapping("/signup")
fun signUp(@Valid @RequestBody request: SignUpRequest): ResponseEntity<ApiResponse<UserResponse>> =
    ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(userService.signUp(request)))
```

### 예외 처리
```kotlin
// CustomException 사용
throw CustomException(ErrorCode.USER_NOT_FOUND)

// ErrorCode에 새 항목 추가
PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
```

### 금액 필드
```kotlin
// Long 타입, 원(KRW) 단위 정수
val price: Long  // O
val price: Double  // X
val price: BigDecimal  // X
```

## 커밋 규칙 (Conventional Commits)

| 타입 | 사용 상황 |
|------|----------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `test` | 테스트 추가/수정 |
| `chore` | 설정, 의존성, 빌드 변경 |

**형식**: `타입(스코프): 설명`

```
feat(user): 회원가입 API 구현
fix(order): 주문 금액 계산 오류 수정
test(product): 상품 서비스 단위 테스트 추가
chore: springdoc-openapi 의존성 추가
```

## Git 워크플로우

1. feature 브랜치 생성: `feature/기능명`
2. 기능 구현
3. `./gradlew build` 빌드 성공 확인
4. 커밋 (Conventional Commits)
5. main 브랜치로 PR 머지

## 브랜치 네이밍

| 유형 | 패턴 | 예시 |
|------|------|------|
| 기능 | `feature/기능명` | `feature/product-crud` |
| 버그 | `fix/버그명` | `fix/order-amount-bug` |
| 핫픽스 | `hotfix/이슈명` | `hotfix/jwt-expiry` |

## 테스트 작성 가이드

- Service 단위 테스트: MockK 사용
- Repository 테스트: `@DataJpaTest` + H2
- Controller 테스트: `@WebMvcTest` + MockK
- 테스트 파일 경로: `src/test/kotlin/...` (프로덕션 패키지 구조 동일)
