'use client'

import { Suspense, useState } from 'react'
import Link from 'next/link'
import { useSearchParams, useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'

// ── 리뷰 작성 폼 (Inner — useSearchParams 사용) ───────────────────────────────

function NewReviewContent() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const queryClient = useQueryClient()
  const { user } = useAuthStore()

  const orderItemIdParam = searchParams.get('orderItemId')
  const orderItemId = orderItemIdParam ? Number(orderItemIdParam) : null

  // orderItemId 없으면 리뷰 관리로 리다이렉트
  if (!orderItemId) {
    router.replace('/my/reviews')
    return null
  }

  const [rating, setRating] = useState(0)
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const MAX_CONTENT = 500
  const MIN_CONTENT = 20
  const isSubmitDisabled = loading || content.length < MIN_CONTENT || rating === 0

  async function handleSubmit() {
    if (!user) {
      router.replace('/signin')
      return
    }
    setLoading(true)
    setError('')
    try {
      await apiFetch('/api/reviews', {
        method: 'POST',
        body: JSON.stringify({ orderItemId, rating, content }),
      })
      queryClient.invalidateQueries({ queryKey: ['my-reviews'] })
      queryClient.invalidateQueries({ queryKey: ['my-delivered-orders'] })
      router.push('/my/reviews')
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === 'UNAUTHORIZED') {
          router.replace('/signin')
          return
        }
        if (err.code === 'REVIEW_ALREADY_EXISTS') {
          setError('이미 작성된 리뷰입니다')
          return
        }
        setError(err.message)
      } else {
        setError('오류가 발생했습니다. 다시 시도해주세요.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="text-xl font-bold mb-6">리뷰 쓰기</h1>

      {/* 상품 정보 카드 (플레이스홀더) */}
      <div className="border border-zinc-200 rounded-lg p-4 mb-6">
        <p className="text-[10px] font-mono text-zinc-400 mb-3">
          ORDER ITEM #{orderItemId}
        </p>
        <div className="flex items-center gap-3">
          <div className="bg-zinc-100 w-14 aspect-[3/4] rounded shrink-0" />
          <p className="text-sm text-zinc-400">상품 정보를 불러오는 중...</p>
        </div>
      </div>

      {/* 별점 섹션 */}
      <div className="mb-6">
        <p className="text-[10px] font-mono tracking-widest text-zinc-400 uppercase mb-2">
          Rating
        </p>
        <div className="flex items-center gap-1">
          {[1, 2, 3, 4, 5].map((star) => (
            <button
              key={star}
              type="button"
              onClick={() => setRating(star)}
              className={`text-4xl cursor-pointer transition-colors leading-none ${
                star <= rating ? 'text-zinc-900' : 'text-zinc-200'
              }`}
            >
              ★
            </button>
          ))}
          <span className="text-sm text-zinc-400 ml-3">
            {rating > 0 ? `${rating}.0 OUT OF 5` : '별점을 선택해주세요'}
          </span>
        </div>
      </div>

      {/* 리뷰 내용 섹션 */}
      <div className="mb-6">
        <p className="text-[10px] font-mono tracking-widest text-zinc-400 uppercase mb-2">
          Review · 최소 20자
        </p>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value.slice(0, MAX_CONTENT))}
          placeholder="상품에 대한 솔직한 리뷰를 작성해주세요."
          className="w-full border border-zinc-200 rounded px-3.5 py-3 text-sm min-h-[120px] resize-none outline-none focus:border-zinc-500 transition-colors"
        />
        <p className="text-xs text-zinc-400 text-right mt-1">
          {content.length} / {MAX_CONTENT}자
        </p>
      </div>

      {/* 사진 첨부 섹션 (UI only) */}
      <div className="mb-4">
        <p className="text-[10px] font-mono tracking-widest text-zinc-400 uppercase mb-2">
          Photos · 선택 · 최대 5장
        </p>
        <button
          type="button"
          className="w-20 aspect-square border border-dashed border-zinc-200 rounded flex items-center justify-center text-zinc-300 text-2xl hover:border-zinc-400 transition-colors"
        >
          +
        </button>
      </div>

      {/* 포인트 안내 배너 */}
      <div className="bg-zinc-50 border border-zinc-200 rounded p-3 text-sm mt-4">
        리뷰 작성 시 300P 적립 · 사진 포함 시 +200P
      </div>

      {/* 에러 메시지 */}
      {error && (
        <p className="text-sm text-red-500 mt-3">{error}</p>
      )}

      {/* 하단 버튼 행 */}
      <div className="flex gap-3 mt-6">
        <Link
          href="/my/reviews"
          className="flex-1 border border-zinc-200 py-3 text-sm rounded text-center text-zinc-700 hover:bg-zinc-50 transition-colors"
        >
          취소
        </Link>
        <button
          type="button"
          onClick={handleSubmit}
          disabled={isSubmitDisabled}
          style={{ flex: 2 }}
          className="bg-zinc-900 text-white py-3 text-sm rounded disabled:opacity-50 hover:bg-zinc-700 transition-colors disabled:cursor-not-allowed"
        >
          {loading ? '등록 중...' : '리뷰 등록 +300p'}
        </button>
      </div>
    </div>
  )
}

// ── 페이지 (Suspense 래핑) ────────────────────────────────────────────────────

export default function NewReviewPage() {
  return (
    <Suspense fallback={<div className="text-center py-8 text-zinc-500">로딩 중...</div>}>
      <NewReviewContent />
    </Suspense>
  )
}
