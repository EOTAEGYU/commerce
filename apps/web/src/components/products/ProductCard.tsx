'use client'

import Link from 'next/link'
import type { components } from '@/types/api'

type ProductResponse = components['schemas']['ProductResponse']

export default function ProductCard({ product }: { product: ProductResponse }) {
  return (
    <Link href={`/products/${product.id}`} className="group block">
      {/* 이미지 */}
      <div className="relative aspect-square overflow-hidden bg-zinc-100">
        {product.imageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={product.imageUrl}
            alt={product.name ?? '상품 이미지'}
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <svg className="h-10 w-10 text-zinc-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
        )}
        {/* 호버 overlay */}
        <div className="absolute inset-0 bg-black/0 transition-colors duration-200 group-hover:bg-black/5" />
        {/* 하트 아이콘 */}
        <button
          onClick={(e) => e.preventDefault()}
          className="absolute bottom-2 right-2 rounded-full p-1 text-white/80 transition-colors hover:text-white drop-shadow"
          aria-label="찜하기"
        >
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" />
          </svg>
        </button>
      </div>

      {/* 텍스트 */}
      <div className="pt-2 pb-1">
        <h2 className="text-sm text-zinc-900 line-clamp-2 leading-snug">{product.name}</h2>
        <p className="mt-1 text-sm font-bold text-zinc-900">
          {(product.price ?? 0).toLocaleString('ko-KR')}원
        </p>
      </div>
    </Link>
  )
}
