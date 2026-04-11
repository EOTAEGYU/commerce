'use client'

import Link from 'next/link'
import { useParams, useRouter } from 'next/navigation'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import type { components } from '@/types/api'

type OrderResponse = components['schemas']['OrderResponse']
type PaymentResponse = components['schemas']['PaymentResponse']

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
    hour: '2-digit', minute: '2-digit',
  })
}

export default function OrderDetailPage() {
  const params = useParams()
  const router = useRouter()
  const queryClient = useQueryClient()
  const { user } = useAuthStore()
  const orderId = Number(params.id)

  const [payMethod, setPayMethod] = useState<'CARD' | 'BANK_TRANSFER'>('CARD')
  const [payError, setPayError] = useState('')
  const [cancelError, setCancelError] = useState('')

  const { data: order, isLoading: orderLoading, isError: orderError } = useQuery({
    queryKey: ['order', orderId],
    queryFn: () => apiFetch<OrderResponse>(`/api/orders/${orderId}`),
    enabled: !!user && !!orderId,
  })

  const { data: payment } = useQuery({
    queryKey: ['payment', orderId],
    queryFn: () => apiFetch<PaymentResponse>(`/api/payments/${orderId}`),
    enabled: !!user && !!orderId && order?.status !== 'PENDING',
  })

  const payMutation = useMutation({
    mutationFn: () =>
      apiFetch<PaymentResponse>('/api/payments', {
        method: 'POST',
        body: JSON.stringify({ orderId, method: payMethod, simulateFailure: false }),
      }),
    onSuccess: () => {
      setPayError('')
      queryClient.invalidateQueries({ queryKey: ['order', orderId] })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        if (error.code === 'UNAUTHORIZED') {
          router.push('/signin')
          return
        }
        if (error.code === 'ORDER_ALREADY_PAID') {
          setPayError('이미 결제된 주문입니다.')
          return
        }
        if (error.code === 'PAYMENT_FAILED') {
          setPayError('결제에 실패했습니다. 다시 시도해 주세요.')
          return
        }
        setPayError(error.message || '결제 중 오류가 발생했습니다.')
      }
    },
  })

  const cancelMutation = useMutation({
    mutationFn: () =>
      apiFetch<OrderResponse>(`/api/orders/${orderId}/cancel`, { method: 'POST' }),
    onSuccess: (data) => {
      setCancelError('')
      queryClient.setQueryData(['order', orderId], data)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        if (error.code === 'UNAUTHORIZED') {
          router.push('/signin')
          return
        }
        setCancelError(error.message || '주문 취소 중 오류가 발생했습니다.')
      }
    },
  })

  const canPay    = order?.status === 'PENDING'
  const canCancel = order?.status === 'PENDING' || order?.status === 'PAID'

  // 비로그인
  if (!user) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-16 text-center">
        <p className="text-zinc-500">로그인 후 이용 가능합니다.</p>
        <Link href="/signin" className="mt-4 inline-block rounded bg-zinc-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-zinc-700">
          로그인
        </Link>
      </main>
    )
  }

  // 로딩
  if (orderLoading) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-8">
        <div className="animate-pulse space-y-4">
          <div className="h-8 w-48 rounded bg-zinc-200" />
          <div className="h-40 rounded-lg bg-zinc-100" />
          <div className="h-24 rounded-lg bg-zinc-100" />
        </div>
      </main>
    )
  }

  // 에러 또는 주문 없음
  if (orderError || !order) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-16 text-center">
        <p className="text-zinc-500">주문을 찾을 수 없습니다.</p>
        <Link href="/orders" className="mt-4 inline-block text-sm text-zinc-700 underline underline-offset-2">
          주문 목록으로
        </Link>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-3xl px-4 py-8">
      {/* 헤더 */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-zinc-900">주문 #{order.id}</h1>
          <p className="mt-1 text-sm text-zinc-500">{formatDate(order.createdAt)}</p>
        </div>
        <StatusBadge status={order.status} />
      </div>

      {/* 주문 상품 목록 */}
      <section className="rounded-lg border border-zinc-200">
        <h2 className="border-b border-zinc-100 px-5 py-3 text-sm font-semibold text-zinc-700">
          주문 상품
        </h2>
        <div className="divide-y divide-zinc-100 px-5">
          {(order.items ?? []).map((item) => (
            <div key={item.id} className="py-4">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm font-medium text-zinc-900">{item.productName}</p>
                  <p className="mt-0.5 text-xs text-zinc-500">{item.optionInfo}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-semibold text-zinc-900">
                    {(item.totalPrice ?? 0).toLocaleString('ko-KR')}원
                  </p>
                  <p className="mt-0.5 text-xs text-zinc-500">
                    {(item.price ?? 0).toLocaleString('ko-KR')}원 × {item.quantity ?? 1}개
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
        <div className="flex items-center justify-between border-t border-zinc-100 px-5 py-4">
          <span className="text-sm font-medium text-zinc-700">총 결제 금액</span>
          <span className="text-lg font-bold text-zinc-900">
            {(order.totalAmount ?? 0).toLocaleString('ko-KR')}원
          </span>
        </div>
      </section>

      {/* 결제 섹션 (PENDING 상태) */}
      {canPay && (
        <section className="mt-4 rounded-lg border border-zinc-200 p-5">
          <h2 className="mb-4 text-sm font-semibold text-zinc-700">결제 수단 선택</h2>
          <div className="flex gap-4">
            {(['CARD', 'BANK_TRANSFER'] as const).map((method) => (
              <label
                key={method}
                className={`flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-lg border py-3 text-sm transition-colors ${
                  payMethod === method
                    ? 'border-zinc-900 bg-zinc-900 font-medium text-white'
                    : 'border-zinc-300 text-zinc-700 hover:border-zinc-500'
                }`}
              >
                <input
                  type="radio"
                  name="payMethod"
                  value={method}
                  checked={payMethod === method}
                  onChange={() => setPayMethod(method)}
                  className="sr-only"
                />
                {method === 'CARD' ? '신용/체크카드' : '계좌이체'}
              </label>
            ))}
          </div>

          {payError && <p className="mt-3 text-sm text-red-500">{payError}</p>}

          <button
            onClick={() => {
              setPayError('')
              payMutation.mutate()
            }}
            disabled={payMutation.isPending}
            className={`mt-4 w-full rounded-lg py-3 text-sm font-medium transition-colors ${
              payMutation.isPending
                ? 'bg-zinc-200 text-zinc-400 cursor-not-allowed'
                : 'bg-zinc-900 text-white hover:bg-zinc-700'
            }`}
          >
            {payMutation.isPending ? '결제 처리 중...' : '결제하기'}
          </button>
        </section>
      )}

      {/* 결제 정보 (PENDING 아닌 상태) */}
      {!canPay && order.status !== 'CANCELLED' && payment && (
        <section className="mt-4 rounded-lg border border-zinc-200 p-5">
          <h2 className="mb-3 text-sm font-semibold text-zinc-700">결제 정보</h2>
          <dl className="space-y-1.5 text-sm">
            <div className="flex justify-between">
              <dt className="text-zinc-500">결제 수단</dt>
              <dd className="font-medium text-zinc-900">
                {payment.method === 'CARD' ? '신용/체크카드' : '계좌이체'}
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-zinc-500">결제 상태</dt>
              <dd className="font-medium text-zinc-900">
                {payment.status === 'COMPLETED' ? '결제 완료' : payment.status}
              </dd>
            </div>
            {payment.pgTransactionId && (
              <div className="flex justify-between">
                <dt className="text-zinc-500">거래 ID</dt>
                <dd className="font-mono text-xs text-zinc-600">{payment.pgTransactionId}</dd>
              </div>
            )}
          </dl>
        </section>
      )}

      {/* 하단 버튼 */}
      <div className="mt-6 flex items-center justify-between">
        <Link
          href="/orders"
          className="text-sm text-zinc-500 underline underline-offset-2 hover:text-zinc-700"
        >
          주문 목록으로
        </Link>

        {canCancel && (
          <div className="flex flex-col items-end gap-1">
            <button
              onClick={() => {
                setCancelError('')
                cancelMutation.mutate()
              }}
              disabled={cancelMutation.isPending}
              className={`rounded-lg border px-4 py-2 text-sm transition-colors ${
                cancelMutation.isPending
                  ? 'border-zinc-200 text-zinc-400 cursor-not-allowed'
                  : 'border-red-200 text-red-600 hover:bg-red-50'
              }`}
            >
              {cancelMutation.isPending ? '취소 처리 중...' : '주문 취소'}
            </button>
            {cancelError && <p className="text-xs text-red-500">{cancelError}</p>}
          </div>
        )}
      </div>
    </main>
  )
}
