# category 도메인

상품 분류를 위한 2depth 계층 카테고리 관리를 담당한다.

## 폴더 구조
```
category/
├── controller/   # CategoryController
├── service/      # CategoryService
├── repository/   # CategoryRepository
├── entity/       # Category
└── dto/          # CategoryCreateRequest, CategoryUpdateRequest, CategoryResponse
```

## Entity
- `Category` : name, parent(Category?), displayOrder(Int)
- `parent = null` 이면 대분류, `parent != null` 이면 소분류
- self-referencing `@ManyToOne` / `@OneToMany` 구조
- BaseEntity 상속 필수

## 주요 ErrorCode (ErrorCode.kt에 추가)
- `CATEGORY_NOT_FOUND` — 존재하지 않는 카테고리
- `CATEGORY_HAS_CHILDREN` — 소분류가 있는 대분류 삭제 시도
- `CATEGORY_IN_USE` — 상품이 속한 카테고리 삭제 시도

## API 엔드포인트
| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST   | /api/categories | 필요(ADMIN) | 카테고리 등록 |
| GET    | /api/categories | 불필요 | 전체 카테고리 트리 조회 |
| PUT    | /api/categories/{id} | 필요(ADMIN) | 카테고리 수정 |
| DELETE | /api/categories/{id} | 필요(ADMIN) | 카테고리 삭제 |

## 응답 구조 (트리)
```json
[
  {
    "id": 1,
    "name": "상의",
    "children": [
      { "id": 3, "name": "반팔티셔츠", "children": [] },
      { "id": 4, "name": "맨투맨", "children": [] }
    ]
  },
  {
    "id": 2,
    "name": "하의",
    "children": [
      { "id": 5, "name": "청바지", "children": [] }
    ]
  }
]
```
