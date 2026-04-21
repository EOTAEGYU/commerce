'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'

// ── 타입 ──────────────────────────────────────────────────────────────────────

interface LikedProduct {
  productId: number
  productName: string
  brandName?: string
  price: number
  salePrice?: number
  imageUrl?: string
  isSoldOut?: boolean
  categoryName?: string
}

type FilterType = 'all' | 'inStock' | 'onSale'

// ── 상품 카드 ─────────────────────────────────────────────────────────────────

function WishlistCard({
  item,
  onToggleLike,
  isPending,
}: {
  item: LikedProduct
  onToggleLike: (productId: number) => void
  isPending: boolean
}) {
  const router = useRouter()
  const isSoldOut = item.isSoldOut ?? false
  const hasSalePrice = item.salePrice != null && item.salePrice < item.price

  return (
    <div>
      {/* 이미지 영역 */}
      <div className="relative aspect-[3/4] bg-zinc-100 rounded mb-2 overflow-hidden">
        {item.imageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={item.imageUrl}
            alt={item.productName}
            className="w-full h-full object-cover rounded"
          />
        ) : null}

        {/* 품절 오버레이 */}
        {isSoldOut && (
          <div className="absolute inset-0 bg-zinc-900/50 flex items-center justify-center">
            <span className="text-white text-xs font-bold">SOLD OUT</span>
          </div>
        )}

        {/* 하트 버튼 */}
        <button
          type="button"
          onClick={() => onToggleLike(item.productId)}
          disabled={isPending}
          className={`absolute top-2 right-2 w-7 h-7 rounded-full bg-white border border-zinc-200 flex items-center justify-center text-sm transition-opacity ${
            isPending ? 'opacity-50' : 'hover:bg-zinc-50'
          }`}
          aria-label="찜 해제"
        >
          <svg
            className="w-4 h-4 text-zinc-900"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={1.5}
            fill="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z"
            />
          </svg>
        </button>
      </div>

      {/* 브랜드명 */}
      <p className="text-[10px] font-mono text-zinc-400 mt-1 truncate">
        {item.brandName ?? 'BRAND'}
      </p>

      {/* 상품명 */}
      <p className="text-sm text-zinc-800 truncate">{item.productName}</p>

      {/* 가격 */}
      <p className={`text-sm mt-0.5 ${isSoldOut ? 'text-zinc-400' : ''}`}>
        {hasSalePrice ? (
          <>
            <span className="font-bold">
              {(item.salePrice ?? 0).toLocaleString('ko-KR')}원
            </span>
            <span className="line-through text-zinc-400 text-xs ml-1">
              {item.price.toLocaleString('ko-KR')}원
            </span>
          </>
        ) : (
          <span className="font-bold">
            {item.price.toLocaleString('ko-KR')}원
          </span>
        )}
      </p>

      {/* 버튼 */}
      {isSoldOut ? (
        <button
          type="button"
          disabled
          className="w-full mt-2 py-2 border border-dashed border-zinc-300 text-xs rounded text-zinc-400 cursor-not-allowed"
        >
          재입고 알림
        </button>
      ) : (
        <button
          type="button"
          onClick={() => router.push(`/products/${item.productId}`)}
          className="w-full mt-2 py-2 border border-zinc-200 text-xs rounded hover:bg-zinc-50 transition-colors"
        >
          담기 +
        </button>
      )}
    </div>
  )
}

// ── 메인 페이지 ───────────────────────────────────────────────────────────────

export default function WishlistPage() {
  const { user } = useAuthStore()
  const router = useRouter()
  const queryClient = useQueryClient()
  const [filterType, setFilterType] = useState<FilterType>('all')

  // 찜한 상품 목록 조회
  const { data, isLoading, error } = useQuery({
    queryKey: ['my-likes'],
    queryFn: () => apiFetch<LikedProduct[]>('/api/likes/my'),
    enabled: !!user,
  })

  // 좋아요 토글 (찜 해제)
  const toggleMutation = useMutation({
    mutationFn: (productId: number) =>
      apiFetch<unknown>(`/api/likes/${productId}`, { method: 'POST' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-likes'] })
    },
    onError: (err) => {
      if (err instanceof ApiError) {
        if (err.code === 'UNAUTHORIZED') {
          router.replace('/signin')
          return
        }
      }
    },
  })

  function handleToggleLike(productId: number) {
    toggleMutation.mutate(productId)
  }

  // 전체 찜 해제: 각 아이템 순차 토글
  function handleClearAll() {
    if (!items.length) return
    items.forEach((item) => toggleMutation.mutate(item.productId))
  }

  // 필터링 로직
  const items = data ?? []
  const filteredItems = items.filter((item) => {
    if (filterType === 'inStock') return !item.isSoldOut
    if (filterType === 'onSale') return item.salePrice != null && item.salePrice < item.price
    return true
  })

  // ── 로딩/에러 처리 ──────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="text-center py-8 text-zinc-500">로딩 중...</div>
    )
  }

  if (error) {
    return (
      <div className="text-center py-8 text-red-500">오류가 발생했습니다.</div>
    )
  }

  // ── 렌더 ────────────────────────────────────────────────────────────────────

  return (
    <div>
      {/* 헤더 */}
      <h1 className="text-xl font-bold mb-4">
        위시리스트
        <span className="text-zinc-400 text-base ml-2">{items.length}</span>
      </h1>

      {/* 필터 행 */}
      <div className="flex gap-2 mb-4 items-center">
        {(
          [
            { key: 'all', label: '전체' },
            { key: 'inStock', label: '품절 제외' },
            { key: 'onSale', label: '세일 중' },
          ] as { key: FilterType; label: string }[]
        ).map(({ key, label }) => (
          <button
            key={key}
            type="button"
            onClick={() => setFilterType(key)}
            className={`text-xs px-3 py-1.5 rounded-full transition-colors ${
              filterType === key
                ? 'bg-zinc-900 text-white'
                : 'border border-zinc-200 text-zinc-600 hover:bg-zinc-50'
            }`}
          >
            {label}
          </button>
        ))}

        {/* spacer */}
        <div className="flex-1" />

        {/* 정렬 (UI only) */}
        <button
          type="button"
          className="border border-zinc-200 text-xs px-3 py-1.5 rounded-full text-zinc-600 hover:bg-zinc-50 transition-colors"
        >
          찜한 순 ▾
        </button>
      </div>

      {/* 빈 상태 */}
      {filteredItems.length === 0 ? (
        <p className="text-sm text-zinc-400 text-center py-16">
          {filterType === 'all'
            ? '위시리스트가 비어있습니다'
            : '해당하는 상품이 없습니다'}
        </p>
      ) : (
        <>
          {/* 상품 그리드 */}
          <div className="grid grid-cols-4 gap-4 mt-2">
            {filteredItems.map((item) => (
              <WishlistCard
                key={item.productId}
                item={item}
                onToggleLike={handleToggleLike}
                isPending={toggleMutation.isPending}
              />
            ))}
          </div>

          {/* 하단 액션 */}
          <div className="flex gap-3 mt-6">
            <button
              type="button"
              onClick={handleClearAll}
              disabled={toggleMutation.isPending}
              className="border border-zinc-200 px-4 py-2 text-sm rounded hover:bg-zinc-50 transition-colors disabled:opacity-50"
            >
              전체 찜 해제
            </button>
          </div>
        </>
      )}
    </div>
  )
}
