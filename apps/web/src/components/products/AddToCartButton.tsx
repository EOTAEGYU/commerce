'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import type { components } from '@/types/api'

type CartResponse = components['schemas']['CartResponse']
type CartItemAddRequest = components['schemas']['CartItemAddRequest']

type Props = {
  productId: number
  selectedOptionId: number | null
}

export default function AddToCartButton({ productId, selectedOptionId }: Props) {
  const router = useRouter()
  const queryClient = useQueryClient()
  const { user } = useAuthStore()
  const [showSuccess, setShowSuccess] = useState(false)
  const [stockError, setStockError] = useState(false)

  const mutation = useMutation({
    mutationFn: (body: CartItemAddRequest) =>
      apiFetch<CartResponse>('/api/cart/items', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] })
      setStockError(false)
      setShowSuccess(true)
      setTimeout(() => setShowSuccess(false), 2000)
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        if (error.code === 'UNAUTHORIZED') {
          router.push('/signin')
          return
        }
        if (error.code === 'OUT_OF_STOCK') {
          setStockError(true)
          return
        }
      }
      setStockError(false)
    },
  })

  function handleClick() {
    if (!user) {
      router.push('/signin')
      return
    }
    if (!selectedOptionId) return
    setStockError(false)
    mutation.mutate({ productId, productOptionId: selectedOptionId, quantity: 1 })
  }

  const isDisabled = !selectedOptionId || mutation.isPending || showSuccess

  let buttonText = '장바구니에 담기'
  if (!selectedOptionId) buttonText = '옵션을 선택하세요'
  else if (mutation.isPending) buttonText = '담는 중...'
  else if (showSuccess) buttonText = '담겼습니다 ✓'

  return (
    <div className="flex flex-col gap-2">
      <button
        onClick={handleClick}
        disabled={isDisabled}
        className={`w-full rounded-lg px-4 py-3 text-sm font-medium transition-colors ${
          isDisabled
            ? 'bg-zinc-200 text-zinc-400 cursor-not-allowed'
            : 'bg-zinc-900 text-white hover:bg-zinc-700'
        }`}
      >
        {buttonText}
      </button>
      {stockError && (
        <p className="text-sm text-red-500 text-center">재고가 부족합니다</p>
      )}
      {mutation.isError && !stockError && (
        <p className="text-sm text-red-500 text-center">오류가 발생했습니다. 다시 시도해 주세요.</p>
      )}
    </div>
  )
}
