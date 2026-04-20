# order 도메인

주문 생성, 상태 관리, 취소, 만료 처리를 담당한다.

## 폴더 구조

```
order/
├── controller/   # OrderController
├── service/      # OrderService
├── scheduler/    # OrderExpirationScheduler (구현 예정)
├── repository/   # OrderRepository, OrderItemRepository
├── entity/       # Order, OrderItem, OrderStatus
└── dto/          # OrderResponse, OrderItemResponse
```

## Entity

### Order
- `userId(Long)`, `status(OrderStatus)`, `totalAmount(Long)`
- `items: MutableList<OrderItem>` — CascadeType.ALL, orphanRemoval=true
- `status`는 var (상태 변경 가능), 나머지 val

### OrderItem (주문 시점 스냅샷)
- `order(Order)` — ManyToOne LAZY
- `productId`, `productOptionId` — 참조용 (val)
- `productName`, `optionInfo`, `price` — 주문 시점 고정값 (val)
- `quantity(Int)` — val

### OrderStatus
```
PENDING → PAID → SHIPPING → DELIVERED
              ↘ CANCELLED
PENDING → CANCELLED (결제 실패 또는 10분 만료)
```

## 주요 비즈니스 규칙

- 주문 생성 시 `ProductOptionRepository.findByIdWithLock(id)` 로 Pessimistic Lock 적용
- 재고 부족 시 전체 실패 (부분 성공 없음)
- 취소 가능: PENDING, PAID 상태만
- PENDING 10분 초과 → 스케줄러가 자동 CANCELLED + 재고 복원
- 주문 생성 완료 후 장바구니 비우기 (`cart.items.clear()`)

## Scheduler

`OrderExpirationScheduler` — 60초 간격으로 실행
- `findExpiredOrdersWithItems` (JOIN FETCH)로 items를 즉시 로딩하여 LazyInitializationException 방지
- `@Transactional` 적용으로 전체 처리를 단일 트랜잭션 내에서 수행
- self-invocation 방지를 위해 `expireSingleOrder` 별도 메서드 제거, 인라인 처리

## Repository

```kotlin
// OrderRepository
fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Order>
fun findByStatusAndCreatedAtBefore(status: OrderStatus, dateTime: LocalDateTime): List<Order>

// 스케줄러용 — items JOIN FETCH로 LazyInit 방지
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items WHERE o.status = :status AND o.createdAt < :dateTime")
fun findExpiredOrdersWithItems(status: OrderStatus, dateTime: LocalDateTime): List<Order>
```

## 주요 ErrorCode

- `CART_EMPTY` — 빈 장바구니로 주문 시도
- `ORDER_NOT_FOUND` — 없는 주문 접근
- `ORDER_NOT_OWNED` — 다른 회원의 주문 접근
- `ORDER_CANNOT_CANCEL` — 취소 불가 상태

## API 엔드포인트

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | /api/orders | 필요 | 주문 생성 (장바구니 전체) |
| GET | /api/orders | 필요 | 내 주문 목록 (최신순) |
| GET | /api/orders/{id} | 필요 | 주문 상세 |
| POST | /api/orders/{id}/cancel | 필요 | 주문 취소 |
