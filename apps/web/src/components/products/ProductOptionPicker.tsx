'use client'

import { useState } from 'react'
import type { components } from '@/types/api'
import AddToCartButton from './AddToCartButton'

type ProductOptionResponse = components['schemas']['ProductOptionResponse']

type Props = {
  options: ProductOptionResponse[]
  productId: number
}

export default function ProductOptionPicker({ options, productId }: Props) {
  const [selectedOptionId, setSelectedOptionId] = useState<number | null>(null)

  if (options.length === 0) {
    return (
      <div className="flex flex-col gap-4">
        <p className="text-sm text-zinc-400">옵션 정보 없음</p>
        <AddToCartButton productId={productId} selectedOptionId={null} />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-2">
        <p className="text-sm font-medium text-zinc-700">옵션 선택</p>
        <div className="flex flex-wrap gap-2">
          {options.map((opt) => (
            <button
              key={opt.id}
              disabled={opt.stock === 0}
              onClick={() => setSelectedOptionId(opt.id ?? null)}
              className={`rounded-md border px-3 py-2 text-sm transition-colors ${
                opt.stock === 0
                  ? 'opacity-40 cursor-not-allowed border-zinc-200 text-zinc-400'
                  : selectedOptionId === opt.id
                  ? 'bg-zinc-900 text-white border-zinc-900'
                  : 'border-zinc-300 text-zinc-700 hover:border-zinc-600'
              }`}
            >
              {opt.size} / {opt.color}
              {opt.stock === 0 && <span className="ml-1 text-xs">(품절)</span>}
            </button>
          ))}
        </div>
      </div>
      <AddToCartButton productId={productId} selectedOptionId={selectedOptionId} />
    </div>
  )
}
