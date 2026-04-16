'use client'

import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import ProductCard from '@/components/products/ProductCard'
import type { components } from '@/types/api'

type PageProductResponse = components['schemas']['PageProductResponse']
type ProductResponse = components['schemas']['ProductResponse']

export default function LikesPage() {
  const user = useAuthStore((s) => s.user)

  const { data, isLoading } = useQuery({
    queryKey: ['likes', 'my'],
    queryFn: () => apiFetch<PageProductResponse>('/api/likes/my?size=40'),
    enabled: !!user,
  })

  if (!user) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-24 text-center">
        <p className="text-zinc-500 mb-4">로그인이 필요한 서비스입니다.</p>
        <Link
          href="/signin"
          className="inline-block rounded-full bg-zinc-900 px-6 py-2 text-sm text-white hover:bg-zinc-700"
        >
          로그인
        </Link>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-24 text-center text-zinc-400 text-sm">
        불러오는 중...
      </div>
    )
  }

  const products: ProductResponse[] = data?.content ?? []

  return (
    <div className="mx-auto max-w-7xl px-4 py-6">
      <div className="mb-4 flex items-baseline gap-2">
        <h1 className="text-base font-bold text-zinc-900">찜한 상품</h1>
        <span className="text-sm text-zinc-400">({products.length})</span>
      </div>

      {products.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-zinc-400">
          <svg className="mb-4 h-12 w-12 text-zinc-200" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" />
          </svg>
          <p className="text-sm">찜한 상품이 없습니다</p>
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} isLiked />
          ))}
        </div>
      )}
    </div>
  )
}
