# 시작 가이드

## 사전 요구사항

| 도구 | 버전 |
|------|------|
| JDK | 21 이상 |
| Docker | 최신 버전 |
| Docker Compose | 포함 (Docker Desktop) |

## 1. 저장소 클론

```bash
git clone <repository-url>
cd commerce
```

## 2. 데이터베이스 실행

PostgreSQL 컨테이너를 Docker Compose로 실행합니다.

```bash
docker-compose up -d
```

| 설정 | 값 |
|------|-----|
| Host | localhost |
| Port | 5432 |
| Database | commerce |
| Username | commerce |
| Password | commerce1234 |

## 3. 서버 실행

```bash
./gradlew bootRun
```

서버가 정상 시작되면 http://localhost:8080 에서 접근 가능합니다.

## 4. 동작 확인

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **회원가입 테스트**:
  ```bash
  curl -X POST http://localhost:8080/api/users/signup \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"password123","name":"테스트"}'
  ```

## 5. 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests "com.example.commerce.user.service.UserServiceTest"
```

테스트는 H2 인메모리 데이터베이스를 사용하므로 PostgreSQL 없이 실행 가능합니다.

## 주의사항

- `ddl-auto: create-drop` 설정으로 서버 재시작 시 스키마가 초기화됩니다.
- JWT 시크릿 키(`jwt.secret`)는 32자 이상이어야 합니다.
- 운영 환경에서는 `application.yaml`의 민감 정보를 환경변수로 분리해야 합니다.
