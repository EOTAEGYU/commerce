'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import type { PageResponse } from '@/types/api'

// ── 인라인 타입 정의 ──────────────────────────────────────────────────────────

interface OrderItem {
  id: number
  productId: number
  productName: string
  optionName?: string
  quantity: number
  price: number
  hasReview?: boolean
  deliveredAt?: string
}

interface Order {
  id: number
  status: 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELED'
  createdAt: string
  items: OrderItem[]
}

interface MyReview {
  id: number
  orderItemId: number
  productId: number
  productName: string
  optionName?: string
  rating: number
  content: string
  createdAt: string
}

type TabType = 'writable' | 'written'

// ── 작성 가능 리뷰 카드 ───────────────────────────────────────────────────────

function WritableReviewCard({
  item,
  orderId,
}: {
  item: OrderItem & { orderId: number }
  orderId: number
}) {
  return (
    <div className="border border-zinc-200 rounded-lg p-4">
      <div className="grid grid-cols-[56px_1fr] gap-3">
        {/* 이미지 플레이스홀더 */}
        <div className="bg-zinc-100 rounded aspect-[3/4] w-14" />
        {/* 상품 정보 */}
        <div className="min-w-0">
          <p className="text-[10px] font-mono text-zinc-400 mb-1">ORDER #{orderId}</p>
          <p className="text-sm font-medium text-zinc-900 truncate">{item.productName}</p>
          {item.optionName && (
            <p className="text-xs text-zinc-400 mt-0.5">{item.optionName}</p>
          )}
          {item.deliveredAt && (
            <p className="text-xs text-zinc-400 mt-0.5">
              배송 {new Date(item.deliveredAt).toLocaleDateString('ko-KR')}
            </p>
          )}
        </div>
      </div>
      <Link
        href={`/my/reviews/new?orderItemId=${item.id}`}
        className="mt-3 block w-full bg-zinc-900 text-white text-sm py-2.5 rounded text-center hover:bg-zinc-700 transition-colors"
      >
        리뷰 쓰기 +300p
      </Link>
    </div>
  )
}

// ── 작성 완료 리뷰 카드 ───────────────────────────────────────────────────────

function WrittenReviewCard({ review }: { review: MyReview }) {
  const fullStars = Math.floor(review.rating)
  const hasHalf = review.rating % 1 !== 0
  const stars = '★'.repeat(fullStars) + (hasHalf ? '☆' : '')

  return (
    <div className="border border-zinc-200 rounded-lg p-4">
      <div className="flex justify-between text-[10px] font-mono text-zinc-400 mb-2">
        <span>{review.productName}{review.optionName ? ` / ${review.optionName}` : ''}</span>
        <span>{new Date(review.createdAt).toLocaleDateString('ko-KR')}</span>
      </div>
      <p className="text-base text-zinc-900 tracking-wide mb-1">{stars}</p>
      <p className="text-sm text-zinc-600 line-clamp-2">{review.content}</p>
    </div>
  )
}

// ── 메인 페이지 ───────────────────────────────────────────────────────────────

export default function ReviewsPage() {
  const { user } = useAuthStore()
  const [activeTab, setActiveTab] = useState<TabType>('writable')

  // 배송완료 주문 조회 (작성 가능 리뷰 추출용)
  const { data: deliveredOrdersData, isLoading: ordersLoading } = useQuery({
    queryKey: ['my-delivered-orders'],
    queryFn: () =>
      apiFetch<PageResponse<Order>>('/api/orders?status=DELIVERED&size=100'),
    enabled: !!user,
    staleTime: 60_000,
    retry: 1,
  })

  // 내가 작성한 리뷰 조회
  const { data: myReviews, isLoading: reviewsLoading } = useQuery({
    queryKey: ['my-reviews'],
    queryFn: () => apiFetch<MyReview[]>('/api/reviews/my'),
    enabled: !!user,
    staleTime: 60_000,
    retry: 1,
  })

  // 작성 가능 아이템 계산: DELIVERED 주문에서 hasReview=false인 items
  const writableItems = (deliveredOrdersData?.content ?? []).flatMap((order) =>
    order.items
      .filter((item) => !item.hasReview)
      .map((item) => ({ ...item, orderId: order.id }))
  )

  const writtenReviews = myReviews ?? []

  const isLoading = activeTab === 'writable' ? ordersLoading : reviewsLoading

  return (
    <div>
      <h1 className="text-xl font-bold mb-4">리뷰 관리</h1>

      {/* 탭 스위처 */}
      <div className="flex border-b border-zinc-200 mb-6">
        <button
          type="button"
          onClick={() => setActiveTab('writable')}
          className={`text-sm px-0 pb-3 mr-6 border-b-2 transition-colors ${
            activeTab === 'writable'
              ? 'border-zinc-900 text-zinc-900 font-bold'
              : 'border-transparent text-zinc-400 cursor-pointer'
          }`}
        >
          작성 가능
          {writableItems.length > 0 && (
            <span className="ml-1.5 text-xs bg-zinc-900 text-white rounded-full px-1.5 py-0.5">
              {writableItems.length}
            </span>
          )}
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('written')}
          className={`text-sm px-0 pb-3 mr-6 border-b-2 transition-colors ${
            activeTab === 'written'
              ? 'border-zinc-900 text-zinc-900 font-bold'
              : 'border-transparent text-zinc-400 cursor-pointer'
          }`}
        >
          작성 완료
          {writtenReviews.length > 0 && (
            <span className="ml-1.5 text-xs bg-zinc-100 text-zinc-600 rounded-full px-1.5 py-0.5">
              {writtenReviews.length}
            </span>
          )}
        </button>
      </div>

      {/* 로딩 */}
      {isLoading && (
        <div className="text-center py-8 text-zinc-500">로딩 중...</div>
      )}

      {/* 작성 가능 탭 */}
      {!isLoading && activeTab === 'writable' && (
        writableItems.length === 0 ? (
          <p className="text-sm text-zinc-400 text-center py-16">
            작성 가능한 리뷰가 없습니다
          </p>
        ) : (
          <div className="grid grid-cols-2 gap-4">
            {writableItems.map((item) => (
              <WritableReviewCard key={item.id} item={item} orderId={item.orderId} />
            ))}
          </div>
        )
      )}

      {/* 작성 완료 탭 */}
      {!isLoading && activeTab === 'written' && (
        writtenReviews.length === 0 ? (
          <p className="text-sm text-zinc-400 text-center py-16">
            작성된 리뷰가 없습니다
          </p>
        ) : (
          <div className="flex flex-col gap-4">
            {writtenReviews.map((review) => (
              <WrittenReviewCard key={review.id} review={review} />
            ))}
          </div>
        )
      )}
    </div>
  )
}
