'use client'

import Link from 'next/link'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import { useRouter } from 'next/navigation'
import type { PageResponse, PointBalanceResponse, CouponResponse } from '@/types/api'

// ── 인라인 타입 정의 ──────────────────────────────────────────────────────────

interface OrderItem {
  id: number
  productName: string
  optionName?: string
  quantity: number
  price: number
  hasReview?: boolean
}

interface OrderSummary {
  id: number
  status: 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELED'
  createdAt: string
  totalAmount: number
  items: OrderItem[]
}

type CouponItem = CouponResponse & { status?: 'ACTIVE' | 'USED' | 'EXPIRED' }

// ── 상태 칩 ───────────────────────────────────────────────────────────────────

const STATUS_LABEL: Record<string, string> = {
  PENDING: '결제 대기',
  PAID: '결제 완료',
  SHIPPED: '배송 중',
  DELIVERED: '배송 완료',
  CANCELED: '취소됨',
}

function StatusChip({ status }: { status: string }) {
  const chipClass =
    status === 'SHIPPED'
      ? 'bg-zinc-900 text-white'
      : 'bg-zinc-100 text-zinc-600'
  return (
    <span className={`text-[10px] font-mono px-2 py-0.5 rounded ${chipClass}`}>
      {STATUS_LABEL[status] ?? status}
    </span>
  )
}

// ── 스탯 카드 ─────────────────────────────────────────────────────────────────

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="border border-zinc-200 rounded-lg p-4">
      <p className="text-[10px] uppercase tracking-widest text-zinc-400 font-mono">{label}</p>
      <p className="text-2xl font-bold mt-1 text-zinc-900">{value}</p>
    </div>
  )
}

// ── 주문 카드 ─────────────────────────────────────────────────────────────────

function OrderCard({
  order,
  onCancel,
  isCanceling,
}: {
  order: OrderSummary
  onCancel: (id: number) => void
  isCanceling: boolean
}) {
  const firstItem = order.items[0]
  const extraCount = order.items.length - 1

  return (
    <div className="border border-zinc-200 rounded-lg overflow-hidden mb-3">
      {/* 헤더 */}
      <div className="flex items-center justify-between bg-zinc-50 px-4 py-2">
        <span className="text-xs font-mono text-zinc-500">
          #{order.id} &middot; {new Date(order.createdAt).toLocaleDateString('ko-KR')}
        </span>
        <StatusChip status={order.status} />
      </div>

      {/* 바디 */}
      <div className="px-4 py-3">
        {firstItem && (
          <div className="flex items-center justify-between mb-3">
            <div className="text-sm text-zinc-800">
              <span className="font-medium">{firstItem.productName}</span>
              {firstItem.optionName && (
                <span className="text-zinc-500 ml-1">({firstItem.optionName})</span>
              )}
              {extraCount > 0 && (
                <span className="text-zinc-400 ml-1 text-xs">외 {extraCount}건</span>
              )}
            </div>
            <span className="text-sm font-semibold text-zinc-900 ml-4 shrink-0">
              {firstItem.price.toLocaleString('ko-KR')}원
            </span>
          </div>
        )}

        {/* 액션 버튼 */}
        <div className="flex gap-2">
          {order.status === 'PENDING' && (
            <>
              <Link
                href="/checkout"
                className="text-xs border border-zinc-900 px-3 py-1 rounded hover:bg-zinc-900 hover:text-white transition-colors"
              >
                결제하기
              </Link>
              <button
                type="button"
                disabled={isCanceling}
                onClick={() => onCancel(order.id)}
                className="text-xs border border-zinc-300 px-3 py-1 rounded text-zinc-600 hover:bg-zinc-50 transition-colors disabled:opacity-50"
              >
                주문취소
              </button>
            </>
          )}
          {order.status === 'PAID' && (
            <button
              type="button"
              disabled={isCanceling}
              onClick={() => onCancel(order.id)}
              className="text-xs border border-zinc-300 px-3 py-1 rounded text-zinc-600 hover:bg-zinc-50 transition-colors disabled:opacity-50"
            >
              주문취소
            </button>
          )}
          {order.status === 'SHIPPED' && (
            <button
              type="button"
              className="text-xs border border-zinc-300 px-3 py-1 rounded text-zinc-600 hover:bg-zinc-50 transition-colors"
            >
              배송조회
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

// ── 메인 페이지 ───────────────────────────────────────────────────────────────

export default function MyPage() {
  const { user } = useAuthStore()
  const router = useRouter()
  const queryClient = useQueryClient()

  // 사용자 정보
  const { data: meData } = useQuery({
    queryKey: ['me'],
    queryFn: () => apiFetch<{ id: number; email: string; name: string; role: string }>('/api/users/me'),
    enabled: !!user,
  })

  // 주문 목록
  const { data: ordersData, isLoading: ordersLoading } = useQuery({
    queryKey: ['my-orders'],
    queryFn: () => apiFetch<PageResponse<OrderSummary>>('/api/orders?page=0&size=20'),
    enabled: !!user,
  })

  // 포인트 잔액
  const { data: pointsData } = useQuery({
    queryKey: ['my-points'],
    queryFn: () => apiFetch<PointBalanceResponse>('/api/points/me'),
    enabled: !!user,
  })

  // 쿠폰 목록
  const { data: couponsData } = useQuery({
    queryKey: ['my-coupons'],
    queryFn: () => apiFetch<CouponItem[]>('/api/coupons/me'),
    enabled: !!user,
  })

  // 주문 취소 mutation
  const cancelMutation = useMutation({
    mutationFn: (orderId: number) =>
      apiFetch<void>(`/api/orders/${orderId}/cancel`, { method: 'POST' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-orders'] })
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        if (error.code === 'UNAUTHORIZED') {
          router.replace('/signin')
          return
        }
        alert(error.message)
      }
    },
  })

  const orders = ordersData?.content ?? []
  const activeOrders = orders.filter((o) =>
    ['PENDING', 'PAID', 'SHIPPED'].includes(o.status)
  )
  const deliveredOrders = orders.filter((o) => o.status === 'DELIVERED')
  const reviewPendingItems = deliveredOrders
    .flatMap((o) =>
      o.items
        .filter((item) => !item.hasReview)
        .map((item) => ({ ...item, orderId: o.id }))
    )
  const activeCoupons = (couponsData ?? []).filter(
    (c) => !c.status || c.status === 'ACTIVE'
  )
  const pointBalance = pointsData?.balance ?? 0
  const displayName = meData?.name ?? user?.name ?? ''

  return (
    <div>
      {/* 1. 인사 헤더 */}
      <h1 className="text-2xl font-bold text-zinc-900 mb-6">
        안녕하세요, {displayName} 님
      </h1>

      {/* 2. 스탯 카드 4개 */}
      <div className="grid grid-cols-4 gap-3 mb-6">
        <StatCard
          label="총 주문"
          value={`${ordersData?.totalElements ?? orders.length}건`}
        />
        <StatCard
          label="포인트"
          value={`${pointBalance.toLocaleString('ko-KR')}P`}
        />
        <StatCard label="쿠폰" value={`${activeCoupons.length}장`} />
        <StatCard label="등급" value="SILVER" />
      </div>

      {/* 3. 쿠폰함 배너 */}
      <div className="bg-zinc-50 border border-zinc-200 rounded p-3 text-sm mb-6">
        <Link href="/my/coupons" className="text-zinc-700 hover:text-zinc-900">
          쿠폰함에서 새 쿠폰을 확인해보세요 →
        </Link>
      </div>

      {/* 4. 진행 중인 주문 */}
      <section className="mb-8">
        <h2 className="text-base font-semibold text-zinc-900 mb-3">진행 중인 주문</h2>
        {ordersLoading ? (
          <p className="text-sm text-zinc-500">로딩 중...</p>
        ) : activeOrders.length === 0 ? (
          <p className="text-sm text-zinc-400 py-4 text-center border border-zinc-200 rounded-lg">
            진행 중인 주문이 없습니다
          </p>
        ) : (
          activeOrders.slice(0, 3).map((order) => (
            <OrderCard
              key={order.id}
              order={order}
              onCancel={(id) => cancelMutation.mutate(id)}
              isCanceling={cancelMutation.isPending}
            />
          ))
        )}
      </section>

      {/* 5. 리뷰 대기 */}
      {reviewPendingItems.length > 0 && (
        <section>
          <h2 className="text-base font-semibold text-zinc-900 mb-3">리뷰 대기</h2>
          <div className="grid grid-cols-3 gap-3">
            {reviewPendingItems.map((item) => (
              <div
                key={item.id}
                className="border border-zinc-200 rounded-lg p-3 grid grid-cols-[56px_1fr] gap-3"
              >
                {/* 이미지 플레이스홀더 */}
                <div className="bg-zinc-100 rounded aspect-[3/4] w-14" />
                <div className="min-w-0 flex flex-col justify-between">
                  <div>
                    <p className="text-sm font-medium text-zinc-800 truncate">
                      {item.productName}
                    </p>
                    <p className="text-[11px] text-zinc-400 font-mono mt-0.5">
                      #{item.orderId}
                    </p>
                  </div>
                  <Link
                    href={`/my/reviews/new?orderItemId=${item.id}`}
                    className="text-xs border border-zinc-900 px-2 py-1 rounded text-zinc-900 hover:bg-zinc-900 hover:text-white transition-colors inline-block mt-2 text-center"
                  >
                    리뷰 쓰기
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
