'use client'

import { useRouter } from 'next/navigation'

export default function KakaoCancel() {
  const router = useRouter()
  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-4">
      <p className="text-lg font-medium">결제가 취소되었습니다.</p>
      <p className="text-sm text-zinc-500">카카오페이 결제를 취소하셨습니다.</p>
      <button
        onClick={() => router.push('/checkout')}
        className="px-6 py-2 bg-zinc-900 text-white rounded-lg text-sm"
      >
        다시 시도
      </button>
    </div>
  )
}
