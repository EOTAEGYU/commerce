package com.example.commerce.coupon.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.coupon.dto.CouponTemplateCreateRequest
import com.example.commerce.coupon.entity.CouponTemplate
import com.example.commerce.coupon.entity.DiscountType
import com.example.commerce.coupon.entity.UserCoupon
import com.example.commerce.coupon.entity.UserCouponStatus
import com.example.commerce.coupon.repository.CouponTemplateRepository
import com.example.commerce.coupon.repository.UserCouponRepository
import com.example.commerce.order.entity.Order
import com.example.commerce.order.entity.OrderItem
import com.example.commerce.order.entity.OrderStatus
import com.example.commerce.product.entity.Product
import com.example.commerce.product.repository.ProductRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockKExtension::class)
class CouponServiceTest {

    @MockK lateinit var couponTemplateRepository: CouponTemplateRepository
    @MockK lateinit var userCouponRepository: UserCouponRepository
    @MockK lateinit var productRepository: ProductRepository

    @InjectMockKs
    lateinit var couponService: CouponService

    private val now: LocalDateTime = LocalDateTime.now()
    private val validFrom: LocalDateTime = now.minusDays(1)
    private val validUntil: LocalDateTime = now.plusDays(30)

    private fun createTemplate(
        id: Long = 1L,
        discountType: DiscountType = DiscountType.FIXED,
        discountValue: Long = 3000L,
        maxDiscountAmount: Long? = null,
        minOrderAmount: Long? = null,
        categoryId: Long? = null,
        totalQuantity: Int? = null,
        issuedCount: Int = 0,
        isActive: Boolean = true,
        validFrom: LocalDateTime = this.validFrom,
        validUntil: LocalDateTime = this.validUntil,
    ) = CouponTemplate(
        name = "테스트 쿠폰",
        discountType = discountType,
        discountValue = discountValue,
        maxDiscountAmount = maxDiscountAmount,
        minOrderAmount = minOrderAmount,
        categoryId = categoryId,
        totalQuantity = totalQuantity,
        issuedCount = issuedCount,
        validFrom = validFrom,
        validUntil = validUntil,
        isActive = isActive,
        id = id,
    )

    private fun createUserCoupon(
        id: Long = 1L,
        userId: Long = 1L,
        couponTemplateId: Long = 1L,
        status: UserCouponStatus = UserCouponStatus.UNUSED,
    ) = UserCoupon(
        userId = userId,
        couponTemplateId = couponTemplateId,
        status = status,
        id = id,
    )

    private fun createOrder(
        id: Long = 1L,
        userId: Long = 1L,
        totalAmount: Long = 20000L,
        productId: Long = 1L,
    ): Order {
        val order = Order(userId = userId, totalAmount = totalAmount, status = OrderStatus.PENDING, id = id)
        order.items.add(
            OrderItem(
                order = order,
                productId = productId,
                productOptionId = 1L,
                productName = "테스트 상품",
                optionInfo = "M / 블랙",
                price = 10000L,
                quantity = 2,
            )
        )
        return order
    }

    private fun createProduct(
        id: Long = 1L,
        categoryId: Long = 10L,
    ) = Product(name = "테스트 상품", price = 10000L, categoryId = categoryId, id = id)

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class IssueCoupon {

        @Test
        fun `정상 입력으로 쿠폰 발급 성공`() {
            // given
            val template = createTemplate()
            val userCoupon = createUserCoupon()
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)
            every { userCouponRepository.findByUserIdAndCouponTemplateId(1L, 1L) } returns null
            every { couponTemplateRepository.save(template) } returns template
            every { userCouponRepository.save(any()) } returns userCoupon

            // when
            val result = couponService.issueCoupon(userId = 1L, templateId = 1L)

            // then
            assertEquals(1L, result.couponTemplateId)
            assertEquals(UserCouponStatus.UNUSED, result.status)
            verify(exactly = 1) { couponTemplateRepository.save(template) }
            verify(exactly = 1) { userCouponRepository.save(any()) }
        }

        @Test
        fun `쿠폰 템플릿 존재하지 않을 시 COUPON_NOT_FOUND 예외 발생`() {
            // given
            every { couponTemplateRepository.findById(99L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> { couponService.issueCoupon(userId = 1L, templateId = 99L) }
            assertEquals(ErrorCode.COUPON_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `비활성화 쿠폰 발급 시 COUPON_NOT_ACTIVE 예외 발생`() {
            // given
            val template = createTemplate(isActive = false)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)

            // when / then
            val ex = assertThrows<CustomException> { couponService.issueCoupon(userId = 1L, templateId = 1L) }
            assertEquals(ErrorCode.COUPON_NOT_ACTIVE, ex.errorCode)
        }

        @Test
        fun `유효기간 이전 쿠폰 발급 시 COUPON_EXPIRED 예외 발생`() {
            // given
            val template = createTemplate(
                validFrom = now.plusDays(1),
                validUntil = now.plusDays(30),
            )
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)

            // when / then
            val ex = assertThrows<CustomException> { couponService.issueCoupon(userId = 1L, templateId = 1L) }
            assertEquals(ErrorCode.COUPON_EXPIRED, ex.errorCode)
        }

        @Test
        fun `유효기간 이후 쿠폰 발급 시 COUPON_EXPIRED 예외 발생`() {
            // given
            val template = createTemplate(
                validFrom = now.minusDays(30),
                validUntil = now.minusDays(1),
            )
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)

            // when / then
            val ex = assertThrows<CustomException> { couponService.issueCoupon(userId = 1L, templateId = 1L) }
            assertEquals(ErrorCode.COUPON_EXPIRED, ex.errorCode)
        }

        @Test
        fun `이미 발급받은 쿠폰 재발급 시 COUPON_ALREADY_ISSUED 예외 발생`() {
            // given
            val template = createTemplate()
            val existingCoupon = createUserCoupon()
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)
            every { userCouponRepository.findByUserIdAndCouponTemplateId(1L, 1L) } returns existingCoupon

            // when / then
            val ex = assertThrows<CustomException> { couponService.issueCoupon(userId = 1L, templateId = 1L) }
            assertEquals(ErrorCode.COUPON_ALREADY_ISSUED, ex.errorCode)
        }

        @Test
        fun `수량 소진된 쿠폰 발급 시 COUPON_QUANTITY_EXHAUSTED 예외 발생`() {
            // given
            val template = createTemplate(totalQuantity = 10, issuedCount = 10)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)
            every { userCouponRepository.findByUserIdAndCouponTemplateId(1L, 1L) } returns null

            // when / then
            val ex = assertThrows<CustomException> { couponService.issueCoupon(userId = 1L, templateId = 1L) }
            assertEquals(ErrorCode.COUPON_QUANTITY_EXHAUSTED, ex.errorCode)
        }

        @Test
        fun `Optimistic Lock 충돌 시 COUPON_QUANTITY_EXHAUSTED 예외 발생`() {
            // given
            val template = createTemplate(totalQuantity = 10, issuedCount = 5)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)
            every { userCouponRepository.findByUserIdAndCouponTemplateId(1L, 1L) } returns null
            every { couponTemplateRepository.save(template) } throws ObjectOptimisticLockingFailureException(
                CouponTemplate::class.java, 1L
            )

            // when / then
            val ex = assertThrows<CustomException> { couponService.issueCoupon(userId = 1L, templateId = 1L) }
            assertEquals(ErrorCode.COUPON_QUANTITY_EXHAUSTED, ex.errorCode)
        }

        @Test
        fun `totalQuantity null인 쿠폰은 수량 제한 없이 발급 성공`() {
            // given
            val template = createTemplate(totalQuantity = null, issuedCount = 9999)
            val userCoupon = createUserCoupon()
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)
            every { userCouponRepository.findByUserIdAndCouponTemplateId(1L, 1L) } returns null
            every { couponTemplateRepository.save(template) } returns template
            every { userCouponRepository.save(any()) } returns userCoupon

            // when
            val result = couponService.issueCoupon(userId = 1L, templateId = 1L)

            // then
            assertEquals(UserCouponStatus.UNUSED, result.status)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class GetMyCoupons {

        @Test
        fun `보유 쿠폰 목록 정상 조회`() {
            // given
            val userCoupon = createUserCoupon()
            val template = createTemplate()
            every { userCouponRepository.findAllByUserId(1L) } returns listOf(userCoupon)
            every { couponTemplateRepository.findAllById(listOf(1L)) } returns listOf(template)

            // when
            val result = couponService.getMyCoupons(userId = 1L)

            // then
            assertEquals(1, result.size)
            assertEquals(1L, result[0].couponTemplateId)
            assertEquals("테스트 쿠폰", result[0].templateName)
        }

        @Test
        fun `보유 쿠폰이 없을 때 빈 목록 반환`() {
            // given
            every { userCouponRepository.findAllByUserId(1L) } returns emptyList()
            every { couponTemplateRepository.findAllById(emptyList()) } returns emptyList()

            // when
            val result = couponService.getMyCoupons(userId = 1L)

            // then
            assertTrue(result.isEmpty())
        }

        @Test
        fun `userCoupon에 매핑되는 템플릿 없을 시 COUPON_NOT_FOUND 예외 발생`() {
            // given
            val userCoupon = createUserCoupon(couponTemplateId = 99L)
            every { userCouponRepository.findAllByUserId(1L) } returns listOf(userCoupon)
            every { couponTemplateRepository.findAllById(listOf(99L)) } returns emptyList()

            // when / then
            val ex = assertThrows<CustomException> { couponService.getMyCoupons(userId = 1L) }
            assertEquals(ErrorCode.COUPON_NOT_FOUND, ex.errorCode)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class ValidateAndApplyCoupon {

        @Test
        fun `FIXED 타입 쿠폰 정상 적용 시 정액 할인금액 반환`() {
            // given
            val userCoupon = createUserCoupon()
            val template = createTemplate(discountType = DiscountType.FIXED, discountValue = 3000L)
            val order = createOrder(totalAmount = 20000L)
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)

            // when
            val (discountAmount, couponId) = couponService.validateAndApplyCoupon(
                userId = 1L, userCouponId = 1L, order = order
            )

            // then
            assertEquals(3000L, discountAmount)
            assertEquals(1L, couponId)
        }

        @Test
        fun `FIXED 타입 쿠폰 할인금액이 주문금액 초과 시 주문금액으로 제한`() {
            // given
            val userCoupon = createUserCoupon()
            val template = createTemplate(discountType = DiscountType.FIXED, discountValue = 50000L)
            val order = createOrder(totalAmount = 20000L)
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)

            // when
            val (discountAmount, _) = couponService.validateAndApplyCoupon(
                userId = 1L, userCouponId = 1L, order = order
            )

            // then
            assertEquals(20000L, discountAmount)
        }

        @Test
        fun `RATE 타입 쿠폰 정상 적용 시 정률 할인금액 반환`() {
            // given
            val userCoupon = createUserCoupon()
            val template = createTemplate(discountType = DiscountType.RATE, discountValue = 10L)
            val order = createOrder(totalAmount = 20000L)
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)

            // when
            val (discountAmount, _) = couponService.validateAndApplyCoupon(
                userId = 1L, userCouponId = 1L, order = order
            )

            // then
            assertEquals(2000L, discountAmount) // 20000 * 10 / 100
        }

        @Test
        fun `RATE 타입 쿠폰 maxDiscountAmount 초과 시 상한 적용`() {
            // given
            val userCoupon = createUserCoupon()
            val template = createTemplate(
                discountType = DiscountType.RATE,
                discountValue = 20L,
                maxDiscountAmount = 3000L,
            )
            val order = createOrder(totalAmount = 20000L)
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)

            // when
            val (discountAmount, _) = couponService.validateAndApplyCoupon(
                userId = 1L, userCouponId = 1L, order = order
            )

            // then
            assertEquals(3000L, discountAmount) // min(4000, 3000)
        }

        @Test
        fun `존재하지 않는 userCouponId로 적용 시 COUPON_NOT_FOUND 예외 발생`() {
            // given
            every { userCouponRepository.findById(99L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> {
                couponService.validateAndApplyCoupon(
                    userId = 1L, userCouponId = 99L, order = createOrder()
                )
            }
            assertEquals(ErrorCode.COUPON_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `다른 사용자 쿠폰 사용 시 COUPON_NOT_OWNED 예외 발생`() {
            // given
            val userCoupon = createUserCoupon(userId = 2L)
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)

            // when / then
            val ex = assertThrows<CustomException> {
                couponService.validateAndApplyCoupon(
                    userId = 1L, userCouponId = 1L, order = createOrder()
                )
            }
            assertEquals(ErrorCode.COUPON_NOT_OWNED, ex.errorCode)
        }

        @Test
        fun `이미 사용된 쿠폰 재사용 시 COUPON_ALREADY_USED 예외 발생`() {
            // given
            val userCoupon = createUserCoupon(status = UserCouponStatus.USED)
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)

            // when / then
            val ex = assertThrows<CustomException> {
                couponService.validateAndApplyCoupon(
                    userId = 1L, userCouponId = 1L, order = createOrder()
                )
            }
            assertEquals(ErrorCode.COUPON_ALREADY_USED, ex.errorCode)
        }

        @Test
        fun `만료된 쿠폰 사용 시 COUPON_EXPIRED 예외 발생`() {
            // given
            val userCoupon = createUserCoupon()
            val template = createTemplate(
                validFrom = now.minusDays(30),
                validUntil = now.minusDays(1),
            )
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)

            // when / then
            val ex = assertThrows<CustomException> {
                couponService.validateAndApplyCoupon(
                    userId = 1L, userCouponId = 1L, order = createOrder()
                )
            }
            assertEquals(ErrorCode.COUPON_EXPIRED, ex.errorCode)
        }

        @Test
        fun `최소 주문금액 미충족 시 COUPON_MIN_AMOUNT_NOT_MET 예외 발생`() {
            // given
            val userCoupon = createUserCoupon()
            val template = createTemplate(minOrderAmount = 30000L)
            val order = createOrder(totalAmount = 20000L)
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)

            // when / then
            val ex = assertThrows<CustomException> {
                couponService.validateAndApplyCoupon(
                    userId = 1L, userCouponId = 1L, order = order
                )
            }
            assertEquals(ErrorCode.COUPON_MIN_AMOUNT_NOT_MET, ex.errorCode)
        }

        @Test
        fun `카테고리 조건 불일치 시 COUPON_CATEGORY_NOT_MET 예외 발생`() {
            // given
            val userCoupon = createUserCoupon()
            val template = createTemplate(categoryId = 99L)
            val order = createOrder(productId = 1L)
            val product = createProduct(id = 1L, categoryId = 10L)
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)
            every { productRepository.findAllById(listOf(1L)) } returns listOf(product)

            // when / then
            val ex = assertThrows<CustomException> {
                couponService.validateAndApplyCoupon(
                    userId = 1L, userCouponId = 1L, order = order
                )
            }
            assertEquals(ErrorCode.COUPON_CATEGORY_NOT_MET, ex.errorCode)
        }

        @Test
        fun `카테고리 조건 일치 시 정상 할인금액 반환`() {
            // given
            val userCoupon = createUserCoupon()
            val template = createTemplate(categoryId = 10L, discountType = DiscountType.FIXED, discountValue = 5000L)
            val order = createOrder(productId = 1L, totalAmount = 20000L)
            val product = createProduct(id = 1L, categoryId = 10L)
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)
            every { productRepository.findAllById(listOf(1L)) } returns listOf(product)

            // when
            val (discountAmount, _) = couponService.validateAndApplyCoupon(
                userId = 1L, userCouponId = 1L, order = order
            )

            // then
            assertEquals(5000L, discountAmount)
        }

        @Test
        fun `validateAndApplyCoupon에서 템플릿 존재하지 않을 시 COUPON_NOT_FOUND 예외 발생`() {
            // given
            val userCoupon = createUserCoupon()
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)
            every { couponTemplateRepository.findById(1L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> {
                couponService.validateAndApplyCoupon(
                    userId = 1L, userCouponId = 1L, order = createOrder()
                )
            }
            assertEquals(ErrorCode.COUPON_NOT_FOUND, ex.errorCode)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class MarkAsUsed {

        @Test
        fun `결제 성공 후 쿠폰 상태 USED로 변경`() {
            // given
            val userCoupon = createUserCoupon(status = UserCouponStatus.UNUSED)
            every { userCouponRepository.findById(1L) } returns Optional.of(userCoupon)

            // when
            couponService.markAsUsed(userCouponId = 1L, orderId = 100L)

            // then
            assertEquals(UserCouponStatus.USED, userCoupon.status)
            assertEquals(100L, userCoupon.usedOrderId)
            assertTrue(userCoupon.usedAt != null)
        }

        @Test
        fun `존재하지 않는 userCouponId로 호출 시 COUPON_NOT_FOUND 예외 발생`() {
            // given
            every { userCouponRepository.findById(99L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> {
                couponService.markAsUsed(userCouponId = 99L, orderId = 100L)
            }
            assertEquals(ErrorCode.COUPON_NOT_FOUND, ex.errorCode)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class CreateCouponTemplate {

        @Test
        fun `관리자가 쿠폰 템플릿 정상 생성`() {
            // given
            val request = CouponTemplateCreateRequest(
                name = "신규 쿠폰",
                discountType = DiscountType.FIXED,
                discountValue = 5000L,
                validFrom = validFrom,
                validUntil = validUntil,
            )
            val savedTemplate = createTemplate(id = 2L, discountValue = 5000L)
            every { couponTemplateRepository.save(any()) } returns savedTemplate

            // when
            val result = couponService.createCouponTemplate(request)

            // then
            assertEquals(2L, result.id)
            assertEquals(5000L, result.discountValue)
            assertEquals(DiscountType.FIXED, result.discountType)
            verify(exactly = 1) { couponTemplateRepository.save(any()) }
        }

        @Test
        fun `RATE 타입 maxDiscountAmount 포함 쿠폰 템플릿 생성`() {
            // given
            val request = CouponTemplateCreateRequest(
                name = "할인율 쿠폰",
                discountType = DiscountType.RATE,
                discountValue = 15L,
                maxDiscountAmount = 5000L,
                validFrom = validFrom,
                validUntil = validUntil,
            )
            val savedTemplate = createTemplate(
                id = 3L,
                discountType = DiscountType.RATE,
                discountValue = 15L,
                maxDiscountAmount = 5000L,
            )
            every { couponTemplateRepository.save(any()) } returns savedTemplate

            // when
            val result = couponService.createCouponTemplate(request)

            // then
            assertEquals(DiscountType.RATE, result.discountType)
            assertEquals(5000L, result.maxDiscountAmount)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class GetAllCouponTemplates {

        @Test
        fun `전체 쿠폰 템플릿 목록 정상 조회`() {
            // given
            val templates = listOf(createTemplate(id = 1L), createTemplate(id = 2L))
            every { couponTemplateRepository.findAll() } returns templates

            // when
            val result = couponService.getAllCouponTemplates()

            // then
            assertEquals(2, result.size)
            assertEquals(1L, result[0].id)
            assertEquals(2L, result[1].id)
        }

        @Test
        fun `쿠폰 템플릿 없을 때 빈 목록 반환`() {
            // given
            every { couponTemplateRepository.findAll() } returns emptyList()

            // when
            val result = couponService.getAllCouponTemplates()

            // then
            assertTrue(result.isEmpty())
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    inner class ToggleCouponTemplate {

        @Test
        fun `활성화 상태 쿠폰 토글 시 비활성화`() {
            // given
            val template = createTemplate(isActive = true)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)

            // when
            val result = couponService.toggleCouponTemplate(id = 1L)

            // then
            assertEquals(false, result.isActive)
        }

        @Test
        fun `비활성화 상태 쿠폰 토글 시 활성화`() {
            // given
            val template = createTemplate(isActive = false)
            every { couponTemplateRepository.findById(1L) } returns Optional.of(template)

            // when
            val result = couponService.toggleCouponTemplate(id = 1L)

            // then
            assertEquals(true, result.isActive)
        }

        @Test
        fun `존재하지 않는 템플릿 토글 시 COUPON_NOT_FOUND 예외 발생`() {
            // given
            every { couponTemplateRepository.findById(99L) } returns Optional.empty()

            // when / then
            val ex = assertThrows<CustomException> { couponService.toggleCouponTemplate(id = 99L) }
            assertEquals(ErrorCode.COUPON_NOT_FOUND, ex.errorCode)
        }
    }
}
