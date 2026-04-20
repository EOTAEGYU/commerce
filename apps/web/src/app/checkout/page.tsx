'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import StepIndicator from '@/components/common/StepIndicator'
import type { components } from '@/types/api'
import type { CouponResponse, PointBalanceResponse } from '@/types/api'

type CartResponse = components['schemas']['CartResponse']
type OrderResponse = components['schemas']['OrderResponse']
type CartItemResponse = components['schemas']['CartItemResponse']

const DELIVERY_NOTES = [
  '문 앞에 놓아주세요',
  '경비실에 맡겨주세요',
  '직접 받을게요',
  '기타',
]

export default function CheckoutPage() {
  const router = useRouter()
  const { user } = useAuthStore()

  // 배송지 상태
  const [recipient, setRecipient] = useState('')
  const [phone, setPhone] = useState('')
  const [postalCode, setPostalCode] = useState('')
  const [address, setAddress] = useState('')
  const [addressDetail, setAddressDetail] = useState('')
  const [deliveryNote, setDeliveryNote] = useState('문 앞에 놓아주세요')

  // 결제 옵션 상태
  const [selectedCouponId, setSelectedCouponId] = useState<number | null>(null)
  const [pointAmount, setPointAmount] = useState(0)
  const [paymentMethod, setPaymentMethod] = useState<'CARD' | 'BANK_TRANSFER'>('CARD')

  // 제출 상태
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [pointError, setPointError] = useState('')

  // 비로그인 리다이렉트
  useEffect(() => {
    if (!user) router.replace('/signin')
  }, [user, router])

  const { data: cart, isLoading: cartLoading } = useQuery({
    queryKey: ['cart'],
    queryFn: () => apiFetch<CartResponse>('/api/cart'),
    enabled: !!user,
  })

  const { data: coupons = [], isLoading: couponsLoading } = useQuery({
    queryKey: ['coupons-me'],
    queryFn: () => apiFetch<CouponResponse[]>('/api/coupons/me'),
    enabled: !!user,
  })

  const { data: pointBalance } = useQuery({
    queryKey: ['points-me'],
    queryFn: () => apiFetch<PointBalanceResponse>('/api/points/me'),
    enabled: !!user,
  })

  if (!user) return null

  const totalAmount = cart?.totalAmount ?? 0
  const items: CartItemResponse[] = cart?.items ?? []

  // 할인 계산
  const couponDiscount = (() => {
    if (!selectedCouponId) return 0
    const coupon = coupons.find((c) => c.id === selectedCouponId)
    if (!coupon) return 0
    if (totalAmount < coupon.minOrderAmount) return 0
    if (coupon.discountType === 'FIXED') return coupon.discountValue
    const rate = Math.floor((totalAmount * coupon.discountValue) / 100)
    return coupon.maxDiscountAmount ? Math.min(rate, coupon.maxDiscountAmount) : rate
  })()

  const finalAmount = Math.max(0, totalAmount - couponDiscount - pointAmount)
  const earnedPoints = Math.floor(finalAmount * 0.01)

  // 포인트 입력 핸들러
  function handlePointInput(value: string) {
    const num = parseInt(value.replace(/[^0-9]/g, ''), 10) || 0
    const balance = pointBalance?.balance ?? 0
    if (num > balance) {
      setPointError('보유 포인트를 초과할 수 없습니다.')
      setPointAmount(balance)
    } else if (num > totalAmount) {
      setPointError('주문 금액을 초과할 수 없습니다.')
      setPointAmount(totalAmount)
    } else {
      setPointError('')
      setPointAmount(num)
    }
  }

  // 결제 버튼 핸들러
  async function handleSubmit() {
    if (!recipient || !phone || !address) {
      setSubmitError('배송지 정보를 모두 입력해주세요.')
      return
    }

    setIsSubmitting(true)
    setSubmitError('')

    try {
      // 1. 주문 생성
      const order = await apiFetch<OrderResponse>('/api/orders', { method: 'POST' })
      // 2. 결제 실행
      await apiFetch('/api/payments', {
        method: 'POST',
        body: JSON.stringify({
          orderId: order.id,
          method: paymentMethod,
          couponId: selectedCouponId,
          pointAmount,
          simulateFailure: false,
        }),
      })
      // 3. 결제 결과 페이지로
      router.push(`/payment/${order.id}`)
    } catch (e) {
      if (e instanceof ApiError) {
        if (e.code === 'UNAUTHORIZED') {
          router.replace('/signin')
          return
        }
        setSubmitError(e.message || '결제 중 오류가 발생했습니다.')
      } else {
        setSubmitError('결제 중 오류가 발생했습니다.')
      }
      setIsSubmitting(false)
    }
  }

  if (cartLoading) {
    return (
      <main className="mx-auto max-w-5xl px-4 py-8">
        <StepIndicator current={2} />
        <div className="text-center py-16 text-zinc-500">로딩 중...</div>
      </main>
    )
  }

  if (!items.length) {
    return (
      <main className="mx-auto max-w-5xl px-4 py-8">
        <StepIndicator current={2} />
        <div className="text-center py-16">
          <p className="text-zinc-500">장바구니가 비어 있습니다.</p>
          <button
            onClick={() => router.push('/cart')}
            className="mt-4 inline-block rounded bg-zinc-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-zinc-700"
          >
            장바구니로 돌아가기
          </button>
        </div>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-5xl px-4 py-8">
      <StepIndicator current={2} />
      <h1 className="mb-6 text-2xl font-bold text-zinc-900">주문/결제</h1>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-[1fr_360px]">
        {/* 좌측 컬럼 */}
        <div className="space-y-8">
          {/* ① 배송지 섹션 */}
          <section className="rounded-lg border border-zinc-200 p-5">
            <h2 className="mb-4 text-base font-semibold text-zinc-900">배송지</h2>
            <div className="space-y-3">
              <div>
                <label className="mb-1 block text-xs font-medium text-zinc-600">
                  수령인 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={recipient}
                  onChange={(e) => setRecipient(e.target.value)}
                  placeholder="이름을 입력하세요"
                  className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 text-sm outline-none focus:border-zinc-900"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-zinc-600">
                  전화번호 <span className="text-red-500">*</span>
                </label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="010-0000-0000"
                  className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 text-sm outline-none focus:border-zinc-900"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-zinc-600">
                  우편번호
                </label>
                <input
                  type="text"
                  value={postalCode}
                  onChange={(e) => setPostalCode(e.target.value)}
                  placeholder="우편번호"
                  className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 text-sm outline-none focus:border-zinc-900"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-zinc-600">
                  주소 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="기본 주소를 입력하세요"
                  className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 text-sm outline-none focus:border-zinc-900"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-zinc-600">
                  상세주소
                </label>
                <input
                  type="text"
                  value={addressDetail}
                  onChange={(e) => setAddressDetail(e.target.value)}
                  placeholder="상세 주소를 입력하세요"
                  className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 text-sm outline-none focus:border-zinc-900"
                />
              </div>
              <div>
                <label className="mb-2 block text-xs font-medium text-zinc-600">
                  배송 메모
                </label>
                <div className="flex flex-wrap gap-2">
                  {DELIVERY_NOTES.map((note) => (
                    <button
                      key={note}
                      type="button"
                      onClick={() => setDeliveryNote(note)}
                      className={`rounded-full px-3 py-1.5 text-xs transition-colors ${
                        deliveryNote === note
                          ? 'bg-zinc-900 text-white'
                          : 'border border-zinc-300 text-zinc-600 hover:bg-zinc-50'
                      }`}
                    >
                      {note}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </section>

          {/* ② 주문 상품 섹션 */}
          <section className="rounded-lg border border-zinc-200 p-5">
            <h2 className="mb-4 text-base font-semibold text-zinc-900">주문 상품</h2>
            <div className="divide-y divide-zinc-100">
              {items.map((item) => (
                <div key={item.id} className="flex items-center gap-3 py-3">
                  <div className="h-12 w-12 flex-shrink-0 overflow-hidden rounded bg-zinc-100">
                    <div className="flex h-full w-full items-center justify-center text-xs text-zinc-400">
                      IMG
                    </div>
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-medium text-zinc-900">
                      상품 #{item.productId}
                    </p>
                    <p className="text-xs text-zinc-500">수량: {item.quantity}</p>
                  </div>
                  <p className="text-sm font-semibold text-zinc-900">
                    {(item.totalPrice ?? 0).toLocaleString('ko-KR')}원
                  </p>
                </div>
              ))}
            </div>
          </section>

          {/* ③ 결제 수단 섹션 */}
          <section className="rounded-lg border border-zinc-200 p-5">
            <h2 className="mb-4 text-base font-semibold text-zinc-900">결제 수단</h2>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setPaymentMethod('CARD')}
                className={`flex-1 rounded-lg py-3 text-sm font-medium transition-colors ${
                  paymentMethod === 'CARD'
                    ? 'bg-zinc-900 text-white'
                    : 'border border-zinc-300 text-zinc-700 hover:bg-zinc-50'
                }`}
              >
                신용/체크카드
              </button>
              <button
                type="button"
                onClick={() => setPaymentMethod('BANK_TRANSFER')}
                className={`flex-1 rounded-lg py-3 text-sm font-medium transition-colors ${
                  paymentMethod === 'BANK_TRANSFER'
                    ? 'bg-zinc-900 text-white'
                    : 'border border-zinc-300 text-zinc-700 hover:bg-zinc-50'
                }`}
              >
                계좌이체
              </button>
            </div>
          </section>
        </div>

        {/* 우측 컬럼 (sticky) */}
        <div className="lg:sticky lg:top-20 lg:self-start space-y-4">
          {/* 쿠폰 선택 */}
          <div className="rounded-lg border border-zinc-200 p-4">
            <h3 className="mb-3 text-sm font-semibold text-zinc-900">쿠폰</h3>
            {couponsLoading ? (
              <p className="text-xs text-zinc-400">로딩 중...</p>
            ) : coupons.length === 0 ? (
              <p className="text-xs text-zinc-400">사용 가능한 쿠폰이 없어요</p>
            ) : (
              <select
                value={selectedCouponId ?? ''}
                onChange={(e) =>
                  setSelectedCouponId(e.target.value ? Number(e.target.value) : null)
                }
                className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-900"
              >
                <option value="">쿠폰 없음</option>
                {coupons.map((coupon) => {
                  const isDisabled = totalAmount < coupon.minOrderAmount
                  const discountLabel =
                    coupon.discountType === 'FIXED'
                      ? `${coupon.discountValue.toLocaleString('ko-KR')}원 할인`
                      : `${coupon.discountValue}% 할인`
                  return (
                    <option
                      key={coupon.id}
                      value={coupon.id}
                      disabled={isDisabled}
                    >
                      {coupon.name} ({discountLabel})
                      {isDisabled
                        ? ` — ${coupon.minOrderAmount.toLocaleString('ko-KR')}원 이상 구매 시`
                        : ''}
                    </option>
                  )
                })}
              </select>
            )}
          </div>

          {/* 포인트 사용 */}
          <div className="rounded-lg border border-zinc-200 p-4">
            <h3 className="mb-1 text-sm font-semibold text-zinc-900">포인트</h3>
            <p className="mb-3 text-xs text-zinc-500">
              보유:{' '}
              <span className="font-medium text-zinc-700">
                {(pointBalance?.balance ?? 0).toLocaleString('ko-KR')}P
              </span>
            </p>
            <div className="flex gap-2">
              <input
                type="text"
                inputMode="numeric"
                value={pointAmount > 0 ? pointAmount.toLocaleString('ko-KR') : ''}
                onChange={(e) => handlePointInput(e.target.value)}
                placeholder="0"
                className="flex-1 rounded-lg border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-900"
              />
              <button
                type="button"
                onClick={() => {
                  const max = Math.min(
                    pointBalance?.balance ?? 0,
                    totalAmount
                  )
                  setPointAmount(max)
                  setPointError('')
                }}
                className="whitespace-nowrap rounded-lg border border-zinc-300 px-3 py-2 text-xs text-zinc-700 hover:bg-zinc-50"
              >
                전액 사용
              </button>
            </div>
            {pointError && (
              <p className="mt-1 text-xs text-red-500">{pointError}</p>
            )}
          </div>

          {/* 결제 내역 */}
          <div className="rounded-lg border border-zinc-200 p-4">
            <h3 className="mb-3 text-sm font-semibold text-zinc-900">결제 내역</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-zinc-600">상품 금액</span>
                <span>{totalAmount.toLocaleString('ko-KR')}원</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-600">배송비</span>
                <span className="text-zinc-500">
                  {totalAmount >= 50000 ? '무료' : '3,000원'}
                </span>
              </div>
              {couponDiscount > 0 && (
                <div className="flex justify-between text-blue-600">
                  <span>쿠폰 할인</span>
                  <span>– {couponDiscount.toLocaleString('ko-KR')}원</span>
                </div>
              )}
              {pointAmount > 0 && (
                <div className="flex justify-between text-blue-600">
                  <span>포인트 사용</span>
                  <span>– {pointAmount.toLocaleString('ko-KR')}원</span>
                </div>
              )}
            </div>

            <div className="my-3 border-t border-zinc-200" />

            <div className="flex justify-between font-semibold">
              <span>최종 결제</span>
              <span className="text-lg text-zinc-900">
                {finalAmount.toLocaleString('ko-KR')}원
              </span>
            </div>

            {earnedPoints > 0 && (
              <p className="mt-1 text-right text-xs text-zinc-500">
                결제 후 +{earnedPoints.toLocaleString('ko-KR')}P 적립 예정
              </p>
            )}
          </div>

          {/* 에러 메시지 */}
          {submitError && (
            <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
              {submitError}
            </p>
          )}

          {/* 결제 버튼 */}
          <button
            type="button"
            onClick={handleSubmit}
            disabled={isSubmitting}
            className={`w-full rounded-lg py-3.5 text-sm font-semibold transition-colors ${
              isSubmitting
                ? 'cursor-not-allowed bg-zinc-200 text-zinc-400'
                : 'bg-zinc-900 text-white hover:bg-zinc-700'
            }`}
          >
            {isSubmitting
              ? '결제 처리 중...'
              : `${finalAmount.toLocaleString('ko-KR')}원 결제하기`}
          </button>
        </div>
      </div>
    </main>
  )
}
