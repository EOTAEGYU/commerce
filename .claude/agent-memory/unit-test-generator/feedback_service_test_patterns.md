---
name: Service Test Patterns (MockK)
description: Established idioms for service-layer unit tests in this project using MockK
type: feedback
---

Service tests use `@ExtendWith(MockKExtension::class)` + `@MockK` + `@InjectMockKs`.

Factory methods must cover all constructor fields required by the entity, using default parameters so individual tests only override what they need.

**Key entity factory patterns:**
- `Order`: requires `userId`, `totalAmount`, `status`, `id`
- `OrderItem`: requires all val fields including `order` reference — build the Order first, then the OrderItem
- `Review`: requires `userId`, `productId`, `orderId`, `orderItemId`, `rating`, `content`, `id`
- `User`: requires `email`, `password`, `name`, `id`

**updateReview dirty-checking note:** `updateReview` mutates the entity in-place (dirty checking) and does NOT call `reviewRepository.save()`. Verify with `verify(exactly = 0) { reviewRepository.save(any()) }`.

**getProductReviews returns Page<ReviewResponse>:** Use `PageImpl(list, pageable, total)` for mock return. Assert `result.totalElements` and `result.isEmpty`.

**userRepository.findById → Optional.empty():** Results in `userName = null` in ReviewResponse. Test this null-safety path explicitly in `getMyReviews`.

**ProductLike factory pattern:** `createProductLike(userId, productId, id)` — all three fields required; `id` defaults to `0L` in the entity but use `1L` in tests to make assertions clear.

**Product factory pattern:** `createProduct(id, name, price, categoryId)` — `options` list defaults to empty `mutableListOf()`, no need to populate for like-domain tests.

**Page.empty() branch in getMyLikedProducts:** When `productLikeRepository.findAllByUserId()` returns an empty list, the service short-circuits with `Page.empty(pageable)` before calling `productRepository.findByIdIn()`. Verify `productRepository.findByIdIn` is never called with `verify(exactly = 0) { productRepository.findByIdIn(any(), any()) }`.

**toggle() side-effect split:** like path calls `save` (verify exactly 1, delete exactly 0); unlike path calls `delete` (verify exactly 1, save exactly 0). Always mock `countByProductId` after the branch to complete the method.

**CouponTemplate factory pattern:** `createTemplate(id, discountType, discountValue, maxDiscountAmount, minOrderAmount, categoryId, totalQuantity, issuedCount, isActive, validFrom, validUntil)` — store `now`, `validFrom = now.minusDays(1)`, `validUntil = now.plusDays(30)` as class-level vals so date-sensitive tests can override cleanly without re-computing.

**UserCoupon factory pattern:** `createUserCoupon(id, userId, couponTemplateId, status)` — status defaults to `UserCouponStatus.UNUSED`.

**Optimistic lock test:** mock `couponTemplateRepository.save(template)` to throw `ObjectOptimisticLockingFailureException(CouponTemplate::class.java, 1L)` — the service catches it and rethrows as `COUPON_QUANTITY_EXHAUSTED`.

**markAsUsed dirty-checking:** the method mutates `userCoupon.status`, `usedOrderId`, `usedAt` in-place — no `save()` call. Assert the field values on the captured entity; no `verify(save)` needed.

**validateAndApplyCoupon — two COUPON_NOT_FOUND throws:** first from `userCouponRepository.findById`, second from `couponTemplateRepository.findById`. Write separate tests for each.

**validateAndApplyCoupon — categoryId null path:** when `template.categoryId == null` the service skips the `productRepository.findAllById` call entirely. No need to mock `productRepository` in the non-category tests.

**UserPoint factory pattern:** `createUserPoint(userId, balance, id)` — `balance` defaults to `5000L` so tests that need an empty wallet override to `0L`.

**PointHistory factory pattern:** `createPointHistory(userId, type, amount, balance, relatedId, id)` — `type` defaults to `USE_PAYMENT`, `relatedId` defaults to `10L` (orderId).

**Order mock for PointService tests:** use `mockk<Order>()` and stub only the fields accessed: `every { order.totalAmount } returns ...` and `every { order.id } returns ...`. Do NOT construct a full `Order` entity.

**earnPoints amount <= 0 early-return:** verify NEITHER `userPointRepository.findByUserId` NOR `pointHistoryRepository.save` is called — both are `exactly = 0`.

**validateAndUsePoints boundary (50%):** `pointAmount == maxUsable` is allowed (not > maxUsable). Write a positive boundary test at exactly 50% to confirm no exception is thrown.

**refundPoints auto-create path:** when `USE_PAYMENT` history exists but `UserPoint` does not, the service calls `getOrCreateUserPoint` which calls `userPointRepository.save`. Mock `findByUserId` to return `null` and `save` to return the new point; verify `save` exactly 1 on `userPointRepository` AND exactly 1 on `pointHistoryRepository`.

**Why:** Ensures consistency with reference test files (OrderServiceTest, ReviewServiceTest, PaymentServiceTest, CouponServiceTest) and catches nullable edge cases at the service boundary.

**How to apply:** Follow these patterns for any service test in this project involving Order/Review/User/ProductLike/Coupon/Point entities.
