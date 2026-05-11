'use client'

import { Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'

function TossFailInner() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const message = searchParams.get('message') ?? '결제에 실패했습니다.'

  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-4">
      <p className="text-lg font-medium">결제에 실패했습니다.</p>
      <p className="text-sm text-zinc-500">{message}</p>
      <button
        onClick={() => router.push('/checkout')}
        className="px-6 py-2 bg-zinc-900 text-white rounded-lg text-sm"
      >
        다시 시도
      </button>
    </div>
  )
}

export default function TossFail() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center">
          <p className="text-sm text-zinc-500">로딩 중...</p>
        </div>
      }
    >
      <TossFailInner />
    </Suspense>
  )
}
