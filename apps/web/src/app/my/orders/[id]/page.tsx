'use client'

import { use } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'

type OrderStatus = 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELED'

interface OrderItem {
  id: number
  productId: number
  productName: string
  optionName?: string
  quantity: number
  price: number
  hasReview?: boolean
}

interface Order {
  id: number
  status: OrderStatus
  createdAt: string
  totalAmount: number
  discountAmount?: number
  pointAmount?: number
  shippingAmount?: number
  couponName?: string
  paymentMethod?: string
  earnedPoints?: number
  shippingAddress?: {
    recipientName: string
    phone: string
    address: string
    zipCode: string
    memo?: string
  }
  items: OrderItem[]
}

const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: '결제대기',
  PAID: '결제완료',
  SHIPPED: '배송중',
  DELIVERED: '배송완료',
  CANCELED: '취소',
}

const STATUS_CHIP_CLASS: Record<OrderStatus, string> = {
  PENDING: 'border border-zinc-200 text-zinc-700',
  PAID: 'border border-zinc-200 text-zinc-700',
  SHIPPED: 'bg-zinc-900 text-white',
  DELIVERED: 'bg-zinc-100 text-zinc-700',
  CANCELED: 'border border-dashed border-zinc-200 text-zinc-400',
}

// 타임라인 단계 순서 (CANCELED 제외)
const TIMELINE_STEPS: OrderStatus[] = ['PAID', 'PAID', 'SHIPPED', 'DELIVERED']
const TIMELINE_LABELS = ['결제완료', '상품준비', '배송중', '배송완료']

function getTimelineStepIndex(status: OrderStatus): number {
  switch (status) {
    case 'PENDING': return -1
    case 'PAID': return 1
    case 'SHIPPED': return 2
    case 'DELIVERED': return 3
    default: return -1
  }
}

export default function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params)
  const router = useRouter()
  const queryClient = useQueryClient()
  const { user } = useAuthStore()

  const { data: order, isLoading, error } = useQuery({
    queryKey: ['my-order', id],
    queryFn: () => apiFetch<Order>(`/api/orders/${id}`),
    enabled: !!user,
    staleTime: 60_000,
    retry: 1,
  })

  const cancelMutation = useMutation({
    mutationFn: () => apiFetch<void>(`/api/orders/${id}/cancel`, { method: 'POST' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-order', id] })
      queryClient.invalidateQueries({ queryKey: ['my-orders'] })
    },
    onError: (err) => {
      if (err instanceof ApiError) {
        if (err.code === 'UNAUTHORIZED') {
          router.replace('/signin')
          return
        }
        alert(err.message)
      }
    },
  })

  if (isLoading) return <div className="text-sm text-zinc-400 py-8">로딩 중...</div>

  if (error || !order) {
    return (
      <div className="text-center py-16">
        <p className="text-sm text-zinc-500 mb-3">주문을 찾을 수 없습니다.</p>
        <Link href="/my/orders" className="text-sm text-zinc-900 underline">
          주문 목록으로
        </Link>
      </div>
    )
  }

  const currentStepIndex = getTimelineStepIndex(order.status)
  const pendingReviewItems = order.items.filter((item) => !item.hasReview)
  const showReviewBanner = order.status === 'DELIVERED' && pendingReviewItems.length > 0

  // 결제 금액 계산
  const itemTotal = order.items.reduce((sum, item) => sum + item.price * item.quantity, 0)
  const shippingAmount = order.shippingAmount ?? 0
  const couponDiscount = order.discountAmount ?? 0
  const pointDiscount = order.pointAmount ?? 0
  const finalAmount = order.totalAmount

  return (
    <div>
      {/* 브레드크럼 */}
      <p className="text-sm text-zinc-400 font-mono mb-2">
        My ›{' '}
        <Link href="/my/orders" className="hover:text-zinc-600 transition-colors">
          주문 내역
        </Link>{' '}
        › #{id}
      </p>

      {/* 헤더 */}
      <div className="flex justify-between items-baseline mb-6">
        <div className="flex items-baseline gap-2">
          <h1 className="text-xl font-bold">주문 #{id}</h1>
          <span className="text-sm text-zinc-400">
            {new Date(order.createdAt).toLocaleDateString('ko-KR')}
          </span>
        </div>
        <span className={`text-xs px-2.5 py-1 rounded-full ${STATUS_CHIP_CLASS[order.status]}`}>
          {STATUS_LABELS[order.status]}
        </span>
      </div>

      {/* 리뷰 배너 (DELIVERED + 미작성 리뷰 있을 때) */}
      {showReviewBanner && (
        <div className="border border-zinc-900 rounded p-3 mb-6 text-sm">
          리뷰를 작성하고 건당 300P를 적립받으세요 · {pendingReviewItems.length}건 남음
        </div>
      )}

      {/* 배송 타임라인 */}
      {order.status === 'CANCELED' ? (
        <div className="my-6 py-4 border border-dashed border-zinc-200 rounded text-center text-sm text-zinc-400">
          취소된 주문
        </div>
      ) : (
        <div className="flex items-center gap-0 my-6">
          {TIMELINE_LABELS.map((label, index) => {
            const isCompleted = currentStepIndex > index
            const isActive = currentStepIndex === index
            return (
              <div key={label} className="flex items-center flex-1 last:flex-none">
                <div className="flex flex-col items-center gap-1">
                  <div
                    className={`w-5 h-5 rounded-full flex items-center justify-center text-xs transition-colors ${
                      isCompleted || isActive
                        ? 'bg-zinc-900 text-white'
                        : 'border-2 border-zinc-200 bg-white'
                    }`}
                  >
                    {isCompleted && '✓'}
                  </div>
                  <span
                    className={`text-xs whitespace-nowrap ${
                      isActive ? 'font-bold text-zinc-900' : 'text-zinc-400'
                    }`}
                  >
                    {label}
                  </span>
                </div>
                {index < TIMELINE_LABELS.length - 1 && (
                  <div
                    className={`h-0.5 flex-1 mx-1 mb-4 ${
                      currentStepIndex > index ? 'bg-zinc-900' : 'bg-zinc-200'
                    }`}
                  />
                )}
              </div>
            )
          })}
        </div>
      )}

      {/* 두 열 레이아웃 */}
      <div className="grid grid-cols-[1fr_280px] gap-6">
        {/* 좌측 */}
        <div>
          {/* 주문 상품 섹션 */}
          <section>
            <h2 className="text-sm font-bold text-zinc-900 mb-2">주문 상품</h2>
            {order.items.map((item) => (
              <div
                key={item.id}
                className="grid grid-cols-[56px_1fr_auto_auto] gap-3 items-center py-3 border-b border-zinc-100 last:border-b-0"
              >
                {/* 이미지 플레이스홀더 */}
                <div className="bg-zinc-100 rounded aspect-[3/4] w-14" />

                {/* 상품명 + 옵션 */}
                <div className="min-w-0">
                  <p className="text-sm font-medium">{item.productName}</p>
                  {item.optionName && (
                    <p className="text-xs text-zinc-400 mt-0.5">{item.optionName}</p>
                  )}
                  <p className="text-xs text-zinc-400 mt-0.5">수량 {item.quantity}</p>
                </div>

                {/* 가격 */}
                <span className="text-sm font-bold whitespace-nowrap">
                  {(item.price * item.quantity).toLocaleString('ko-KR')}원
                </span>

                {/* 액션 버튼 */}
                <div className="flex flex-col gap-1.5 items-end">
                  {order.status === 'PENDING' && (
                    <>
                      <Link
                        href="/checkout"
                        className="text-xs border border-zinc-200 px-2.5 py-1.5 rounded hover:bg-zinc-50 whitespace-nowrap"
                      >
                        결제하기
                      </Link>
                      <button
                        onClick={() => cancelMutation.mutate()}
                        disabled={cancelMutation.isPending}
                        className="text-xs border border-zinc-200 px-2.5 py-1.5 rounded hover:bg-zinc-50 whitespace-nowrap disabled:opacity-50"
                      >
                        주문취소
                      </button>
                    </>
                  )}
                  {order.status === 'PAID' && (
                    <button
                      onClick={() => cancelMutation.mutate()}
                      disabled={cancelMutation.isPending}
                      className="text-xs border border-zinc-200 px-2.5 py-1.5 rounded hover:bg-zinc-50 whitespace-nowrap disabled:opacity-50"
                    >
                      주문취소
                    </button>
                  )}
                  {order.status === 'SHIPPED' && (
                    <button className="text-xs border border-zinc-200 px-2.5 py-1.5 rounded hover:bg-zinc-50 whitespace-nowrap">
                      배송조회
                    </button>
                  )}
                  {order.status === 'DELIVERED' && (
                    item.hasReview ? (
                      <Link
                        href="/my/reviews"
                        className="text-xs border border-zinc-200 px-2.5 py-1.5 rounded hover:bg-zinc-50 whitespace-nowrap"
                      >
                        리뷰 수정
                      </Link>
                    ) : (
                      <Link
                        href={`/my/reviews/new?orderItemId=${item.id}`}
                        className="bg-zinc-900 text-white text-xs px-3 py-2 rounded whitespace-nowrap hover:bg-zinc-700 transition-colors"
                      >
                        리뷰 쓰기 +300p
                      </Link>
                    )
                  )}
                  {order.status === 'CANCELED' && (
                    <Link
                      href={`/products/${item.productId}`}
                      className="text-xs border border-zinc-200 px-2.5 py-1.5 rounded hover:bg-zinc-50 whitespace-nowrap"
                    >
                      재구매
                    </Link>
                  )}
                </div>
              </div>
            ))}
          </section>

          {/* 배송지 섹션 */}
          {order.shippingAddress && (
            <section className="border border-zinc-200 rounded-lg p-4 mt-6">
              <h2 className="text-sm font-bold text-zinc-900 mb-3">배송지</h2>
              <div className="space-y-1 text-sm text-zinc-700">
                <p>
                  <span className="font-medium">{order.shippingAddress.recipientName}</span>
                  <span className="text-zinc-400 mx-2">·</span>
                  {order.shippingAddress.phone}
                </p>
                <p>
                  {order.shippingAddress.address}
                  <span className="text-zinc-400 ml-1">({order.shippingAddress.zipCode})</span>
                </p>
                {order.shippingAddress.memo && (
                  <p className="text-xs text-zinc-400 mt-1">{order.shippingAddress.memo}</p>
                )}
              </div>
            </section>
          )}
        </div>

        {/* 우측 aside */}
        <aside className="space-y-3">
          {/* 결제 내역 카드 */}
          <div className="border border-zinc-200 rounded-lg p-4">
            <h2 className="text-sm font-bold text-zinc-900 mb-3">결제 내역</h2>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between text-zinc-600">
                <span>상품금액</span>
                <span>{itemTotal.toLocaleString('ko-KR')}원</span>
              </div>
              <div className="flex justify-between text-zinc-600">
                <span>배송비</span>
                <span>{shippingAmount > 0 ? `${shippingAmount.toLocaleString('ko-KR')}원` : '무료'}</span>
              </div>
              {order.couponName && couponDiscount > 0 && (
                <div className="flex justify-between text-red-500">
                  <span>쿠폰 ({order.couponName})</span>
                  <span>– {couponDiscount.toLocaleString('ko-KR')}원</span>
                </div>
              )}
              {pointDiscount > 0 && (
                <div className="flex justify-between text-red-500">
                  <span>포인트 사용</span>
                  <span>– {pointDiscount.toLocaleString('ko-KR')}원</span>
                </div>
              )}
              <hr className="border-zinc-100 my-1" />
              <div className="flex justify-between font-bold text-zinc-900">
                <span>최종 결제</span>
                <span>{finalAmount.toLocaleString('ko-KR')}원</span>
              </div>
              {order.paymentMethod && (
                <p className="text-xs text-zinc-400 mt-1">{order.paymentMethod}</p>
              )}
              {order.earnedPoints != null && order.earnedPoints > 0 && (
                <p className="text-xs text-zinc-500 mt-1">
                  적립 예정 포인트: {order.earnedPoints.toLocaleString('ko-KR')}P
                </p>
              )}
            </div>
          </div>

          {/* 영수증 버튼 */}
          <button className="w-full border border-zinc-200 rounded-lg px-4 py-2.5 text-xs text-zinc-600 hover:bg-zinc-50 transition-colors">
            영수증 · 현금영수증
          </button>

          {/* 상태별 취소/환불 버튼 */}
          {(order.status === 'PENDING' || order.status === 'PAID') && (
            <button
              onClick={() => cancelMutation.mutate()}
              disabled={cancelMutation.isPending}
              className="w-full border border-zinc-200 rounded-lg px-4 py-2.5 text-xs text-zinc-600 hover:bg-zinc-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              주문 취소
            </button>
          )}
          {order.status === 'SHIPPED' && (
            <button
              disabled
              className="w-full border border-dashed border-zinc-200 rounded-lg px-4 py-2.5 text-xs text-zinc-400 cursor-not-allowed"
            >
              배송 시작 후 취소 불가 (CS 문의)
            </button>
          )}
          {order.status === 'DELIVERED' && (
            <button className="w-full border border-zinc-200 rounded-lg px-4 py-2.5 text-xs text-zinc-600 hover:bg-zinc-50 transition-colors">
              교환 · 반품 신청
            </button>
          )}
        </aside>
      </div>
    </div>
  )
}
