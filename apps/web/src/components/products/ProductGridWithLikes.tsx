'use client'

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import ProductCard from './ProductCard'
import type { components } from '@/types/api'

type ProductResponse = components['schemas']['ProductResponse']

type Props = {
  products: ProductResponse[]
}

export default function ProductGridWithLikes({ products }: Props) {
  const user = useAuthStore((s) => s.user)
  const productIds = products.map((p) => p.id!).filter(Boolean)

  const { data: likeStatusMap } = useQuery({
    queryKey: ['likes', 'status', productIds],
    queryFn: () =>
      apiFetch<Record<string, boolean>>(
        `/api/likes/status?productIds=${productIds.join(',')}`
      ),
    enabled: !!user && productIds.length > 0,
  })

  if (products.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-zinc-400">
        <svg className="mb-4 h-12 w-12 text-zinc-200" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
        </svg>
        <p className="text-sm">상품이 없습니다</p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4">
      {products.map((p) => (
        <ProductCard
          key={p.id}
          product={p}
          isLiked={likeStatusMap?.[String(p.id)] ?? false}
        />
      ))}
    </div>
  )
}
