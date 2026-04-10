# common 모듈

공통으로 사용되는 인프라 코드. 모든 도메인에서 참조한다.

## 구현 완료 목록

### BaseEntity
- 모든 JPA Entity의 부모 클래스
- `createdAt`, `updatedAt` 자동 관리 (`@EntityListeners(AuditingEntityListener::class)`)
- Entity 정의 시 반드시 상속: `class User(...) : BaseEntity()`

### ApiResponse<T>
- 모든 API 응답의 공통 래퍼
- `ApiResponse.success(data)` — 성공 응답
- `ApiResponse.error(errorCode)` — 실패 응답
- Controller의 반환 타입은 항상 `ResponseEntity<ApiResponse<T>>`

### ErrorCode
- 에러 코드 열거형. 새 에러 추가 시 여기에 항목 추가
- 현재 정의: `INTERNAL_SERVER_ERROR`, `INVALID_INPUT`, `ENTITY_NOT_FOUND`
- 도메인별 에러 코드도 이 파일에 추가 (예: `USER_NOT_FOUND`, `DUPLICATE_EMAIL`)

### CustomException
- `throw CustomException(ErrorCode.XXX)` 형태로 사용
- GlobalExceptionHandler가 자동으로 ApiResponse.error로 변환

### GlobalExceptionHandler
- `@RestControllerAdvice`로 전역 예외 처리
- CustomException, MethodArgumentNotValidException, Exception 처리
- 직접 수정할 일 없음

## 주의사항
- ErrorCode에 새 항목 추가할 때는 적절한 HttpStatus와 한국어 메시지 함께 정의
