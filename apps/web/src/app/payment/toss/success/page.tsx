'use client'

import { Suspense, useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { apiFetch } from '@/lib/api/client'

function TossSuccessInner() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const paymentKey = searchParams.get('paymentKey')
    const orderId = searchParams.get('orderId')
    const amount = searchParams.get('amount')

    if (!paymentKey || !orderId || !amount) {
      setError('잘못된 접근입니다.')
      return
    }

    apiFetch('/api/payments/toss/confirm', {
      method: 'POST',
      body: JSON.stringify({
        paymentKey,
        orderId: Number(orderId),
        amount: Number(amount),
      }),
    })
      .then(() => router.push(`/payment/${orderId}`))
      .catch(() => setError('결제 승인 중 오류가 발생했습니다.'))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (error) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4">
        <p className="text-red-600">{error}</p>
        <button
          onClick={() => router.push('/checkout')}
          className="px-4 py-2 bg-zinc-900 text-white rounded-lg text-sm"
        >
          다시 시도
        </button>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-3">
      <div className="w-8 h-8 border-2 border-zinc-900 border-t-transparent rounded-full animate-spin" />
      <p className="text-sm text-zinc-600">토스페이 결제를 처리하는 중...</p>
    </div>
  )
}

export default function TossSuccess() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex flex-col items-center justify-center gap-3">
          <div className="w-8 h-8 border-2 border-zinc-900 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-zinc-600">로딩 중...</p>
        </div>
      }
    >
      <TossSuccessInner />
    </Suspense>
  )
}
