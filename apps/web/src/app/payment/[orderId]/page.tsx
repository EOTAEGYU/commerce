'use client'

import { useEffect } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import type { PaymentResultResponse } from '@/types/api'

const METHOD_LABELS: Record<string, string> = {
  CARD: '신용/체크카드',
  BANK_TRANSFER: '계좌이체',
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  return d.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function PaymentResultPage() {
  const params = useParams()
  const router = useRouter()
  const { user } = useAuthStore()
  const queryClient = useQueryClient()

  const orderId = params?.orderId as string | undefined

  // 비로그인 리다이렉트
  useEffect(() => {
    if (!user) router.replace('/signin')
  }, [user, router])

  const { data: payment, isLoading } = useQuery({
    queryKey: ['payment-result', orderId],
    queryFn: () => apiFetch<PaymentResultResponse>(`/api/payments/${orderId}`),
    refetchInterval: (query) => {
      const data = query.state.data
      if (data?.status === 'COMPLETED' || data?.status === 'FAILED') return false
      return 1000
    },
    enabled: !!user && !!orderId,
  })

  // 결제 완료 시 장바구니 무효화
  useEffect(() => {
    if (payment?.status === 'COMPLETED') {
      queryClient.invalidateQueries({ queryKey: ['cart'] })
    }
  }, [payment?.status, queryClient])

  if (!user) return null

  const isPending = isLoading || payment?.status === 'PENDING'

  // ─── 진행중 ───────────────────────────────────────────────────────────────
  if (isPending) {
    return (
      <main className="mx-auto flex min-h-[60vh] max-w-lg flex-col items-center justify-center gap-6 px-4 py-16">
        <div className="h-12 w-12 animate-spin rounded-full border-2 border-zinc-200 border-t-zinc-900" />
        <div className="text-center">
          <p className="text-lg font-semibold text-zinc-900">결제 진행 중…</p>
          <p className="mt-1 text-sm text-zinc-500">
            창을 닫지 마세요. 자동으로 이동합니다.
          </p>
        </div>
        {orderId && (
          <p className="rounded-full bg-zinc-100 px-4 py-1.5 text-xs font-mono text-zinc-500">
            ORDER #{orderId}
          </p>
        )}
      </main>
    )
  }

  // ─── 완료 ─────────────────────────────────────────────────────────────────
  if (payment?.status === 'COMPLETED') {
    const discountTotal = (payment.discountAmount ?? 0) + (payment.pointAmount ?? 0)

    return (
      <main className="mx-auto max-w-lg px-4 py-16">
        <div className="flex flex-col items-center gap-4 text-center">
          {/* 완료 아이콘 */}
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-zinc-900">
            <svg
              className="h-8 w-8 text-white"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2.5}
                d="M5 13l4 4L19 7"
              />
            </svg>
          </div>
          <div>
            <h1 className="text-2xl font-bold text-zinc-900">주문이 완료되었어요!</h1>
            <p className="mt-1 text-sm text-zinc-500">
              ORDER #{payment.orderId}
              {payment.createdAt && ` · ${formatDate(payment.createdAt)}`}
            </p>
          </div>
        </div>

        {/* 결제 상세 카드 */}
        <div className="mt-8 rounded-lg border border-zinc-200 p-5">
          <div className="space-y-3 text-sm">
            <div className="flex justify-between">
              <span className="text-zinc-600">결제 금액</span>
              <span className="font-semibold text-zinc-900">
                {(payment.amount ?? 0).toLocaleString('ko-KR')}원
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-zinc-600">결제 수단</span>
              <span className="text-zinc-900">
                {METHOD_LABELS[payment.method ?? ''] ?? payment.method}
              </span>
            </div>
            {discountTotal > 0 && (
              <div className="flex justify-between text-blue-600">
                <span>쿠폰/포인트</span>
                <span>– {discountTotal.toLocaleString('ko-KR')}원</span>
              </div>
            )}
          </div>

          {(payment.earnedPoints ?? 0) > 0 && (
            <>
              <div className="my-4 border-t border-zinc-200" />
              <div className="flex items-center justify-between">
                <span className="text-sm text-zinc-600">적립 포인트</span>
                <span className="text-2xl font-bold text-zinc-900">
                  +{(payment.earnedPoints ?? 0).toLocaleString('ko-KR')}P
                </span>
              </div>
            </>
          )}
        </div>

        {/* 액션 버튼 */}
        <div className="mt-6 flex gap-3">
          <button
            onClick={() => router.push(`/orders/${payment.orderId}`)}
            className="flex-1 rounded-lg border border-zinc-300 py-3 text-sm font-medium text-zinc-700 hover:bg-zinc-50"
          >
            주문 상세 보기
          </button>
          <button
            onClick={() => router.push('/')}
            className="flex-1 rounded-lg bg-zinc-900 py-3 text-sm font-medium text-white hover:bg-zinc-700"
          >
            쇼핑 계속하기
          </button>
        </div>
      </main>
    )
  }

  // ─── 실패 ─────────────────────────────────────────────────────────────────
  return (
    <main className="mx-auto max-w-lg px-4 py-16">
      <div className="flex flex-col items-center gap-4 text-center">
        {/* 실패 아이콘 */}
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
          <svg
            className="h-8 w-8 text-red-600"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2.5}
              d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"
            />
          </svg>
        </div>
        <div>
          <h1 className="text-2xl font-bold text-zinc-900">결제에 실패했어요</h1>
          <p className="mt-1 text-sm text-zinc-500">
            {payment?.failureReason ?? '카드사 승인 거절'}
          </p>
        </div>
      </div>

      {/* 안내 사항 */}
      <div className="mt-8 rounded-lg border border-red-100 bg-red-50 p-5">
        <p className="mb-3 text-sm font-semibold text-red-800">확인해주세요:</p>
        <ul className="space-y-1.5 text-sm text-red-700">
          <li className="flex items-start gap-2">
            <span className="mt-0.5 text-red-400">•</span>
            카드 유효기간이 지나지 않았는지
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 text-red-400">•</span>
            한도 또는 잔액이 충분한지
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 text-red-400">•</span>
            해외 결제 차단이 걸려있지 않은지
          </li>
        </ul>
      </div>

      <p className="mt-4 text-center text-xs text-zinc-500">
        주문은 30분간 보관되며 재시도할 수 있어요.
      </p>

      {/* 액션 버튼 */}
      <div className="mt-6 flex gap-3">
        <button
          onClick={() => router.push('/checkout')}
          className="flex-1 rounded-lg border border-zinc-300 py-3 text-sm font-medium text-zinc-700 hover:bg-zinc-50"
        >
          결제 수단 변경
        </button>
        <button
          onClick={() => router.push('/checkout')}
          className="flex-1 rounded-lg bg-zinc-900 py-3 text-sm font-medium text-white hover:bg-zinc-700"
        >
          다시 시도
        </button>
      </div>
    </main>
  )
}
