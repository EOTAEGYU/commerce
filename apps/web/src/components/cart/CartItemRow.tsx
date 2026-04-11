'use client'

import Image from 'next/image'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '@/lib/api/client'
import type { components } from '@/types/api'

type CartResponse = components['schemas']['CartResponse']
type CartItemResponse = components['schemas']['CartItemResponse']

export type CartItemWithProduct = CartItemResponse & {
  productName: string
  optionInfo: string
  imageUrl?: string
}

type Props = {
  item: CartItemWithProduct
}

export default function CartItemRow({ item }: Props) {
  const queryClient = useQueryClient()

  const updateMutation = useMutation({
    mutationFn: ({ id, quantity }: { id: number; quantity: number }) =>
      apiFetch<CartResponse>(`/api/cart/items/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ quantity }),
      }),
    onSuccess: (data) => {
      queryClient.setQueryData(['cart'], data)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) =>
      apiFetch<CartResponse>(`/api/cart/items/${id}`, { method: 'DELETE' }),
    onSuccess: (data) => {
      queryClient.setQueryData(['cart'], data)
    },
  })

  const isPending = updateMutation.isPending || deleteMutation.isPending

  function handleDecrease() {
    if (!item.id) return
    if ((item.quantity ?? 1) <= 1) return
    updateMutation.mutate({ id: item.id, quantity: (item.quantity ?? 1) - 1 })
  }

  function handleIncrease() {
    if (!item.id) return
    updateMutation.mutate({ id: item.id, quantity: (item.quantity ?? 1) + 1 })
  }

  function handleDelete() {
    if (!item.id) return
    deleteMutation.mutate(item.id)
  }

  const outOfStock = updateMutation.error instanceof ApiError && updateMutation.error.code === 'OUT_OF_STOCK'

  return (
    <div className="flex gap-4 py-5">
      {/* 이미지 */}
      <div className="relative h-20 w-20 flex-shrink-0 overflow-hidden rounded-md bg-zinc-100">
        {item.imageUrl ? (
          <Image
            src={item.imageUrl}
            alt={item.productName}
            fill
            className="object-cover"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-xs text-zinc-400">
            No Image
          </div>
        )}
      </div>

      {/* 상품 정보 */}
      <div className="flex flex-1 flex-col justify-between">
        <div className="flex items-start justify-between gap-2">
          <div>
            <p className="text-sm font-medium text-zinc-900 line-clamp-1">{item.productName}</p>
            <p className="mt-0.5 text-xs text-zinc-500">{item.optionInfo}</p>
            <p className="mt-1 text-xs text-zinc-500">
              개당 {(item.price ?? 0).toLocaleString('ko-KR')}원
            </p>
          </div>
          <button
            onClick={handleDelete}
            disabled={isPending}
            className="text-xs text-zinc-400 hover:text-zinc-700 disabled:opacity-40"
          >
            삭제
          </button>
        </div>

        <div className="flex items-center justify-between">
          {/* 수량 조절 */}
          <div className="flex items-center gap-1">
            <button
              onClick={handleDecrease}
              disabled={isPending || (item.quantity ?? 1) <= 1}
              className="flex h-7 w-7 items-center justify-center rounded border border-zinc-300 text-sm text-zinc-700 hover:bg-zinc-50 disabled:opacity-40"
            >
              −
            </button>
            <span className="w-8 text-center text-sm font-medium">{item.quantity ?? 1}</span>
            <button
              onClick={handleIncrease}
              disabled={isPending}
              className="flex h-7 w-7 items-center justify-center rounded border border-zinc-300 text-sm text-zinc-700 hover:bg-zinc-50 disabled:opacity-40"
            >
              +
            </button>
          </div>

          {/* 소계 */}
          <p className="text-sm font-semibold text-zinc-900">
            {(item.totalPrice ?? 0).toLocaleString('ko-KR')}원
          </p>
        </div>

        {outOfStock && (
          <p className="mt-1 text-xs text-red-500">재고가 부족합니다</p>
        )}
      </div>
    </div>
  )
}
