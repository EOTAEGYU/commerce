---
name: Controller Test Patterns
description: Established patterns for @WebMvcTest controller tests in this project
type: feedback
---

Use `authentication(UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER"))))` via `SecurityMockMvcRequestPostProcessors.authentication` for user auth in controller tests. For admin tests use `ROLE_ADMIN`.

Always include `with(csrf())` on mutating requests (POST, PUT, DELETE).

Import `tools.jackson.databind.ObjectMapper` (not `com.fasterxml.jackson.databind.ObjectMapper`) — this project uses Jackson 3.x via Spring Boot 4.

Import `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` (not `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest`) — Spring Boot 4 moved this package.

**Why:** Spring Boot 4 reorganized packages; using old imports causes compile errors.

**How to apply:** Every time a controller test is generated, use these exact import paths.

ErrorCode HTTP status mappings for Review domain:
- `REVIEW_NOT_FOUND` → 404
- `REVIEW_ALREADY_EXISTS` → 409
- `REVIEW_NOT_OWNED` → 403
- `ORDER_NOT_DELIVERED` → 400
- `INVALID_RATING` → 400
- `ORDER_ITEM_NOT_FOUND` → 404

ErrorCode HTTP status mappings for Like domain:
- `PRODUCT_NOT_FOUND` → 404

Like domain controller test notes:
- `getLikeStatus` uses `@RequestParam productIds: List<Long>` — pass as `?productIds=1,2,3` in test URL
- Map keys in jsonPath for Long keys use string form: `$.data.1`, `$.data.2`
- Toggle (POST) needs `with(csrf())`; GET endpoints do not
