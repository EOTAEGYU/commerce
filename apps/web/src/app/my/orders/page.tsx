'use client'

import { Suspense, useState } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import PaginationBar from '@/components/products/PaginationBar'

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

interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

const STATUS_LABELS: Record<OrderStatus | 'ALL', string> = {
  ALL: '전체',
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

function OrdersContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const queryClient = useQueryClient()
  const { user } = useAuthStore()

  const statusParam = searchParams.get('status') as OrderStatus | null
  const periodParam = searchParams.get('period')

  const [selectedStatus, setSelectedStatus] = useState<OrderStatus | 'ALL'>(statusParam ?? 'ALL')
  const [selectedPeriod, setSelectedPeriod] = useState<string>(periodParam ?? '3')
  const [page, setPage] = useState(0)

  function updateFilters(status: OrderStatus | 'ALL', period: string) {
    const params = new URLSearchParams()
    if (status !== 'ALL') params.set('status', status)
    if (period !== '3') params.set('period', period)
    router.replace(`/my/orders${params.toString() ? `?${params.toString()}` : ''}`)
  }

  function handleStatusChange(status: OrderStatus | 'ALL') {
    setSelectedStatus(status)
    setPage(0)
    updateFilters(status, selectedPeriod)
  }

  function handlePeriodChange(period: string) {
    setSelectedPeriod(period)
    setPage(0)
    updateFilters(selectedStatus, period)
  }

  const queryParams = new URLSearchParams()
  if (selectedStatus !== 'ALL') queryParams.set('status', selectedStatus)
  queryParams.set('page', String(page))
  queryParams.set('size', '10')

  const { data, isLoading, error } = useQuery({
    queryKey: ['my-orders', selectedStatus, selectedPeriod, page],
    queryFn: () => apiFetch<PageResponse<Order>>(`/api/orders?${queryParams.toString()}`),
    enabled: !!user,
    staleTime: 60_000,
    retry: 1,
  })

  const cancelMutation = useMutation({
    mutationFn: (orderId: number) =>
      apiFetch<void>(`/api/orders/${orderId}/cancel`, { method: 'POST' }),
    onSuccess: () => {
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

  if (isLoading) return <div className="text-center py-8 text-zinc-500">로딩 중...</div>
  if (error) return <div className="text-center py-8 text-red-500">오류가 발생했습니다.</div>

  const orders = data?.content ?? []
  const totalPages = data?.totalPages ?? 0

  return (
    <div>
      <h1 className="text-xl font-bold mb-4">주문 내역</h1>

      {/* 필터 행 */}
      <div className="flex gap-2 items-center mb-4 flex-wrap">
        {(['ALL', 'PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELED'] as const).map((status) => (
          <button
            key={status}
            onClick={() => handleStatusChange(status)}
            className={`text-xs px-3 py-1.5 rounded-full transition-colors ${
              selectedStatus === status
                ? 'bg-zinc-900 text-white'
                : 'border border-zinc-200 text-zinc-600 hover:bg-zinc-50'
            }`}
          >
            {STATUS_LABELS[status]}
          </button>
        ))}
        <div className="flex-1" />
        <select
          value={selectedPeriod}
          onChange={(e) => handlePeriodChange(e.target.value)}
          className="border border-zinc-200 rounded px-2 py-1 text-xs text-zinc-600 bg-white"
        >
          <option value="3">최근 3개월</option>
          <option value="6">최근 6개월</option>
          <option value="12">최근 12개월</option>
        </select>
      </div>

      {/* 주문 목록 */}
      {orders.length === 0 ? (
        <div className="text-sm text-zinc-400 text-center py-16">주문 내역이 없습니다.</div>
      ) : (
        orders.map((order) => (
          <div key={order.id} className="border border-zinc-200 rounded-lg overflow-hidden mb-3">
            {/* 카드 헤더 */}
            <div className="flex justify-between items-center px-4 py-2.5 bg-zinc-50 border-b border-zinc-200">
              <div className="flex items-center gap-2">
                <span className="text-xs font-mono text-zinc-500">
                  #{order.id} · {new Date(order.createdAt).toLocaleDateString('ko-KR')}
                </span>
                <span className={`text-xs px-2 py-0.5 rounded-full ${STATUS_CHIP_CLASS[order.status]}`}>
                  {STATUS_LABELS[order.status]}
                </span>
              </div>
              <Link
                href={`/my/orders/${order.id}`}
                className="text-xs text-zinc-400 hover:text-zinc-600 transition-colors"
              >
                상세 ▸
              </Link>
            </div>

            {/* 카드 바디 */}
            <div className="px-4 py-3">
              {order.items.map((item) => (
                <div
                  key={item.id}
                  className="grid grid-cols-[56px_1fr_auto_auto] gap-3 items-center py-2 border-t first:border-t-0 border-zinc-100"
                >
                  {/* 이미지 플레이스홀더 */}
                  <div className="bg-zinc-100 rounded aspect-[3/4] w-14" />

                  {/* 상품명 + 옵션 */}
                  <div className="min-w-0">
                    <p className="text-sm font-medium truncate">{item.productName}</p>
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
                          onClick={() => cancelMutation.mutate(order.id)}
                          disabled={cancelMutation.isPending}
                          className="text-xs border border-zinc-200 px-2.5 py-1.5 rounded hover:bg-zinc-50 whitespace-nowrap disabled:opacity-50"
                        >
                          주문취소
                        </button>
                      </>
                    )}
                    {order.status === 'PAID' && (
                      <button
                        onClick={() => cancelMutation.mutate(order.id)}
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
                          리뷰수정
                        </Link>
                      ) : (
                        <Link
                          href={`/my/reviews/new?orderItemId=${item.id}`}
                          className="text-xs border border-zinc-200 px-2.5 py-1.5 rounded hover:bg-zinc-50 whitespace-nowrap"
                        >
                          리뷰쓰기
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
            </div>
          </div>
        ))
      )}

      {/* 페이지네이션 */}
      <PaginationBar
        totalPages={totalPages}
        currentPage={page}
        basePath="/my/orders"
      />
    </div>
  )
}

export default function OrdersPage() {
  return (
    <Suspense fallback={<div className="text-center py-8 text-zinc-500">로딩 중...</div>}>
      <OrdersContent />
    </Suspense>
  )
}
