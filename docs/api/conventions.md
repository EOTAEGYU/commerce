# API 공통 규칙

## 응답 형식

모든 API 응답은 `ApiResponse<T>` 형식으로 반환됩니다.

### 성공 응답
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

### 에러 응답
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "DUPLICATE_EMAIL",
    "message": "이미 사용 중인 이메일입니다."
  }
}
```

## HTTP 상태 코드

| 상황 | 상태 코드 |
|------|----------|
| 조회 성공 | 200 OK |
| 생성 성공 | 201 Created |
| 잘못된 요청 (입력 오류) | 400 Bad Request |
| 인증 필요 | 401 Unauthorized |
| 리소스 없음 | 404 Not Found |
| 충돌 (중복) | 409 Conflict |
| 서버 오류 | 500 Internal Server Error |

## 인증

인증이 필요한 API는 요청 헤더에 JWT 토큰을 포함해야 합니다.

```
Authorization: Bearer <accessToken>
```

- 토큰은 `POST /api/users/signin` 응답의 `accessToken` 값
- 유효 기간: 24시간
- 토큰 누락/만료 시 → 401 반환

## 입력 유효성 검사

요청 body가 유효하지 않을 때 400 에러와 함께 어떤 필드가 문제인지 반환합니다.

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_INPUT",
    "message": "잘못된 입력값입니다."
  }
}
```

## Swagger UI

API 상세 스펙(요청/응답 스키마, 파라미터)은 Swagger UI에서 확인 가능합니다.

- **URL**: http://localhost:8080/swagger-ui.html
- **JSON 스펙**: http://localhost:8080/v3/api-docs

Swagger에서 인증이 필요한 API를 테스트할 때는 우측 상단 **Authorize** 버튼을 클릭하고 `Bearer <토큰값>` 형식으로 입력합니다.
