'use client'

import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import type { components } from '@/types/api'

type OrderResponse = components['schemas']['OrderResponse']

const STATUS_CONFIG: Record<string, { label: string; cls: string }> = {
  PENDING:   { label: '결제 대기',  cls: 'bg-yellow-100 text-yellow-800' },
  PAID:      { label: '결제 완료',  cls: 'bg-blue-100 text-blue-800' },
  SHIPPING:  { label: '배송 중',    cls: 'bg-indigo-100 text-indigo-800' },
  DELIVERED: { label: '배송 완료',  cls: 'bg-green-100 text-green-800' },
  CANCELLED: { label: '취소됨',    cls: 'bg-zinc-100 text-zinc-500' },
}

function StatusBadge({ status }: { status?: string }) {
  const cfg = STATUS_CONFIG[status ?? ''] ?? { label: status ?? '-', cls: 'bg-zinc-100 text-zinc-500' }
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${cfg.cls}`}>
      {cfg.label}
    </span>
  )
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric',
  })
}

export default function OrdersPage() {
  const { user } = useAuthStore()

  const { data: orders = [], isLoading, isError } = useQuery({
    queryKey: ['orders'],
    queryFn: () => apiFetch<OrderResponse[]>('/api/orders'),
    enabled: !!user,
  })

  // 비로그인
  if (!user) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-16 text-center">
        <p className="text-zinc-500">로그인 후 이용 가능합니다.</p>
        <Link
          href="/signin"
          className="mt-4 inline-block rounded bg-zinc-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-zinc-700"
        >
          로그인
        </Link>
      </main>
    )
  }

  // 로딩
  if (isLoading) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-8">
        <h1 className="mb-6 text-2xl font-bold text-zinc-900">주문 내역</h1>
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="animate-pulse rounded-lg border border-zinc-200 p-5">
              <div className="flex items-center justify-between">
                <div className="h-4 w-32 rounded bg-zinc-200" />
                <div className="h-5 w-16 rounded-full bg-zinc-200" />
              </div>
              <div className="mt-3 h-3 w-48 rounded bg-zinc-200" />
            </div>
          ))}
        </div>
      </main>
    )
  }

  // 에러
  if (isError) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-16 text-center">
        <p className="text-zinc-500">주문 내역을 불러오지 못했습니다.</p>
      </main>
    )
  }

  // 주문 없음
  if (!orders.length) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-16 text-center">
        <h1 className="mb-4 text-2xl font-bold text-zinc-900">주문 내역</h1>
        <p className="text-zinc-500">주문 내역이 없습니다.</p>
        <Link
          href="/"
          className="mt-4 inline-block rounded bg-zinc-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-zinc-700"
        >
          쇼핑 시작하기
        </Link>
      </main>
    )
  }

  // 최신순 정렬
  const sorted = [...orders].reverse()

  return (
    <main className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold text-zinc-900">주문 내역</h1>
      <div className="space-y-3">
        {sorted.map((order) => (
          <Link
            key={order.id}
            href={`/orders/${order.id}`}
            className="block rounded-lg border border-zinc-200 p-5 transition-colors hover:border-zinc-400 hover:bg-zinc-50"
          >
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold text-zinc-900">주문 #{order.id}</span>
              <StatusBadge status={order.status} />
            </div>
            <p className="mt-1 text-xs text-zinc-500">{formatDate(order.createdAt)}</p>
            <div className="mt-3 flex items-center justify-between">
              <span className="text-xs text-zinc-500">
                상품 {(order.items ?? []).length}종
              </span>
              <span className="text-sm font-semibold text-zinc-900">
                {(order.totalAmount ?? 0).toLocaleString('ko-KR')}원
              </span>
            </div>
          </Link>
        ))}
      </div>
    </main>
  )
}
