'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { useQuery, useQueries, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import CartItemRow from '@/components/cart/CartItemRow'
import type { CartItemWithProduct } from '@/components/cart/CartItemRow'
import type { components } from '@/types/api'

type CartResponse = components['schemas']['CartResponse']
type OrderResponse = components['schemas']['OrderResponse']
type ProductResponse = components['schemas']['ProductResponse']

const EMPTY_CART: CartResponse = { id: 0, userId: 0, items: [], totalAmount: 0 }

export default function CartPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const { user } = useAuthStore()
  const [orderError, setOrderError] = useState('')

  const { data: cart, isLoading: cartLoading } = useQuery({
    queryKey: ['cart'],
    queryFn: () => apiFetch<CartResponse>('/api/cart'),
    enabled: !!user,
  })

  // 장바구니 아이템의 고유 productId 목록
  type CartItem = NonNullable<CartResponse['items']>[number]
  const productIds = [...new Set((cart?.items ?? []).map((i: CartItem) => i.productId).filter((id): id is number => !!id))]

  const productQueries = useQueries({
    queries: productIds.map((id) => ({
      queryKey: ['product', id],
      queryFn: () => apiFetch<ProductResponse>(`/api/products/${id}`),
      staleTime: 5 * 60 * 1000,
    })),
  })

  const productsLoading = productQueries.some((q) => q.isLoading)

  // 상품 맵 빌드
  const productMap = new Map<number, ProductResponse>()
  productQueries.forEach((q) => {
    if (q.data?.id) productMap.set(q.data.id, q.data)
  })

  // CartItem에 상품명/옵션 정보 보강
  type ProductOption = NonNullable<ProductResponse['options']>[number]
  const enrichedItems: CartItemWithProduct[] = (cart?.items ?? []).map((item: CartItem) => {
    const product = productMap.get(item.productId ?? 0)
    const option = product?.options?.find((o: ProductOption) => o.id === item.productOptionId)
    return {
      ...item,
      productName: product?.name ?? `상품 #${item.productId}`,
      optionInfo: option ? `${option.size ?? ''} / ${option.color ?? ''}` : '-',
      imageUrl: product?.imageUrl,
    }
  })

  const orderMutation = useMutation({
    mutationFn: () =>
      apiFetch<OrderResponse>('/api/orders', { method: 'POST' }),
    onSuccess: (order) => {
      queryClient.setQueryData(['cart'], EMPTY_CART)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      router.push(`/orders/${order.id}`)
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        if (error.code === 'UNAUTHORIZED') {
          router.push('/signin')
          return
        }
        if (error.code === 'OUT_OF_STOCK') {
          setOrderError('일부 상품의 재고가 부족합니다. 수량을 확인해 주세요.')
          return
        }
      }
      setOrderError('주문 생성 중 오류가 발생했습니다. 다시 시도해 주세요.')
    },
  })

  // 비로그인
  if (!user) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-16 text-center">
        <p className="text-zinc-500">로그인 후 이용 가능합니다.</p>
        <Link
          href="/signin"
          className="mt-4 inline-block rounded bg-zinc-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-zinc-700"
        >
          로그인
        </Link>
      </main>
    )
  }

  // 로딩
  if (cartLoading || productsLoading) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-8">
        <h1 className="mb-6 text-2xl font-bold text-zinc-900">장바구니</h1>
        <div className="divide-y divide-zinc-100">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="flex gap-4 py-5 animate-pulse">
              <div className="h-20 w-20 flex-shrink-0 rounded-md bg-zinc-200" />
              <div className="flex-1 space-y-2 py-1">
                <div className="h-4 w-2/3 rounded bg-zinc-200" />
                <div className="h-3 w-1/3 rounded bg-zinc-200" />
                <div className="h-3 w-1/4 rounded bg-zinc-200" />
              </div>
            </div>
          ))}
        </div>
      </main>
    )
  }

  // 빈 장바구니
  if (!enrichedItems.length) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-16 text-center">
        <p className="text-zinc-500">장바구니가 비어 있습니다.</p>
        <Link
          href="/"
          className="mt-4 inline-block rounded bg-zinc-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-zinc-700"
        >
          쇼핑 계속하기
        </Link>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold text-zinc-900">장바구니</h1>

      <div className="divide-y divide-zinc-100 rounded-lg border border-zinc-200">
        <div className="px-4">
          {enrichedItems.map((item) => (
            <CartItemRow key={item.id} item={item} />
          ))}
        </div>
      </div>

      {/* 주문 요약 */}
      <div className="mt-6 rounded-lg border border-zinc-200 p-5">
        <div className="flex items-center justify-between">
          <span className="text-sm text-zinc-600">총 상품 금액</span>
          <span className="text-lg font-bold text-zinc-900">
            {(cart?.totalAmount ?? 0).toLocaleString('ko-KR')}원
          </span>
        </div>

        {orderError && (
          <p className="mt-3 text-sm text-red-500">{orderError}</p>
        )}

        <button
          onClick={() => {
            setOrderError('')
            orderMutation.mutate()
          }}
          disabled={orderMutation.isPending}
          className={`mt-4 w-full rounded-lg py-3 text-sm font-medium transition-colors ${
            orderMutation.isPending
              ? 'bg-zinc-200 text-zinc-400 cursor-not-allowed'
              : 'bg-zinc-900 text-white hover:bg-zinc-700'
          }`}
        >
          {orderMutation.isPending ? '주문 생성 중...' : '주문하기'}
        </button>
      </div>
    </main>
  )
}
