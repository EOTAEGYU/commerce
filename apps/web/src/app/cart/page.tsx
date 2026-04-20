'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useQuery, useQueries } from '@tanstack/react-query'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import CartItemRow from '@/components/cart/CartItemRow'
import StepIndicator from '@/components/common/StepIndicator'
import type { CartItemWithProduct } from '@/components/cart/CartItemRow'
import type { components } from '@/types/api'
import { useMutation, useQueryClient } from '@tanstack/react-query'

type CartResponse = components['schemas']['CartResponse']
type ProductResponse = components['schemas']['ProductResponse']
type CartItemAddRequest = components['schemas']['CartItemAddRequest']

export default function CartPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const { user } = useAuthStore()

  const { data: cart, isLoading: cartLoading } = useQuery({
    queryKey: ['cart'],
    queryFn: () => apiFetch<CartResponse>('/api/cart'),
    enabled: !!user,
  })

  // 찜 상품 조회 (로그인 상태 + 장바구니 빈 경우에만 활용)
  const { data: likedProducts } = useQuery({
    queryKey: ['likes-my'],
    queryFn: () => apiFetch<ProductResponse[]>('/api/likes/my'),
    enabled: !!user,
  })

  type CartItem = NonNullable<CartResponse['items']>[number]
  const productIds = [
    ...new Set(
      (cart?.items ?? [])
        .map((i: CartItem) => i.productId)
        .filter((id): id is number => !!id)
    ),
  ]

  const productQueries = useQueries({
    queries: productIds.map((id) => ({
      queryKey: ['product', id],
      queryFn: () => apiFetch<ProductResponse>(`/api/products/${id}`),
      staleTime: 5 * 60 * 1000,
    })),
  })

  const productsLoading = productQueries.some((q) => q.isLoading)

  const productMap = new Map<number, ProductResponse>()
  productQueries.forEach((q) => {
    if (q.data?.id) productMap.set(q.data.id, q.data)
  })

  type ProductOption = NonNullable<ProductResponse['options']>[number]
  const enrichedItems: CartItemWithProduct[] = (cart?.items ?? []).map(
    (item: CartItem) => {
      const product = productMap.get(item.productId ?? 0)
      const option = product?.options?.find(
        (o: ProductOption) => o.id === item.productOptionId
      )
      return {
        ...item,
        productName: product?.name ?? `상품 #${item.productId}`,
        optionInfo: option ? `${option.size ?? ''} / ${option.color ?? ''}` : '-',
        imageUrl: product?.imageUrl,
      }
    }
  )

  // 찜 상품 장바구니 담기 mutation
  const addToCartMutation = useMutation({
    mutationFn: (body: CartItemAddRequest) =>
      apiFetch<CartResponse>('/api/cart/items', {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    onSuccess: (data) => {
      queryClient.setQueryData(['cart'], data)
    },
    onError: (error) => {
      if (error instanceof ApiError && error.code === 'UNAUTHORIZED') {
        router.push('/signin')
      }
    },
  })

  // 비로그인
  if (!user) {
    return (
      <main className="mx-auto max-w-5xl px-4 py-16 text-center">
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
      <main className="mx-auto max-w-5xl px-4 py-8">
        <StepIndicator current={1} />
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
      <main className="mx-auto max-w-5xl px-4 py-16">
        <StepIndicator current={1} />
        <div className="text-center">
          <p className="text-zinc-500">장바구니가 비어 있습니다.</p>
          <Link
            href="/"
            className="mt-4 inline-block rounded bg-zinc-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-zinc-700"
          >
            쇼핑 계속하기
          </Link>
        </div>

        {/* 찜한 상품에서 바로 담기 */}
        {likedProducts && likedProducts.length > 0 && (
          <section className="mt-12">
            <h2 className="mb-4 text-lg font-semibold text-zinc-900">
              찜한 상품에서 바로 담기
            </h2>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              {likedProducts.map((product) => {
                const firstOption = product.options?.[0]
                return (
                  <div
                    key={product.id}
                    className="rounded-lg border border-zinc-200 p-3"
                  >
                    <div className="mb-2 aspect-square overflow-hidden rounded bg-zinc-100">
                      {product.imageUrl ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          src={product.imageUrl}
                          alt={product.name ?? ''}
                          className="h-full w-full object-cover"
                        />
                      ) : (
                        <div className="flex h-full w-full items-center justify-center text-xs text-zinc-400">
                          No Image
                        </div>
                      )}
                    </div>
                    <p className="line-clamp-1 text-xs font-medium text-zinc-900">
                      {product.name}
                    </p>
                    <p className="mt-0.5 text-xs text-zinc-500">
                      {(product.price ?? 0).toLocaleString('ko-KR')}원
                    </p>
                    <button
                      onClick={() => {
                        if (!product.id || !firstOption?.id) return
                        addToCartMutation.mutate({
                          productId: product.id,
                          productOptionId: firstOption.id,
                          quantity: 1,
                        })
                      }}
                      disabled={addToCartMutation.isPending || !firstOption}
                      className="mt-2 w-full rounded border border-zinc-300 py-1.5 text-xs text-zinc-700 hover:bg-zinc-50 disabled:opacity-40"
                    >
                      담기 +
                    </button>
                  </div>
                )
              })}
            </div>
          </section>
        )}
      </main>
    )
  }

  const totalAmount = cart?.totalAmount ?? 0

  return (
    <main className="mx-auto max-w-5xl px-4 py-8">
      <StepIndicator current={1} />
      <h1 className="mb-6 text-2xl font-bold text-zinc-900">장바구니</h1>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-[1fr_360px]">
        {/* 좌측 — 상품 테이블 */}
        <div>
          {/* 헤더 행 */}
          <div className="hidden grid-cols-[1fr_80px_100px] gap-4 border-b border-zinc-200 pb-2 lg:grid">
            <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
              상품
            </span>
            <span className="text-center text-xs font-semibold uppercase tracking-wider text-zinc-400">
              수량
            </span>
            <span className="text-right text-xs font-semibold uppercase tracking-wider text-zinc-400">
              합계
            </span>
          </div>

          <div className="divide-y divide-zinc-100">
            {enrichedItems.map((item) => (
              <CartItemRow key={item.id} item={item} />
            ))}
          </div>

          <div className="mt-4">
            <Link
              href="/"
              className="text-sm text-zinc-500 underline-offset-2 hover:text-zinc-900 hover:underline"
            >
              ← 쇼핑 계속하기
            </Link>
          </div>
        </div>

        {/* 우측 — sticky Summary */}
        <div className="lg:sticky lg:top-20 lg:self-start">
          <div className="rounded-lg border border-zinc-200 p-5">
            <h2 className="mb-4 text-base font-semibold text-zinc-900">
              주문 요약
            </h2>

            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-zinc-600">상품 금액</span>
                <span className="text-zinc-900">
                  {totalAmount.toLocaleString('ko-KR')}원
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-600">배송비</span>
                <span className="text-zinc-500">
                  {totalAmount >= 50000 ? '무료 (5만원 이상)' : '3,000원'}
                </span>
              </div>
            </div>

            <div className="my-4 border-t border-zinc-200" />

            <div className="flex justify-between">
              <span className="font-semibold text-zinc-900">주문 합계</span>
              <span className="text-lg font-bold text-zinc-900">
                {totalAmount.toLocaleString('ko-KR')}원
              </span>
            </div>

            <button
              onClick={() => router.push('/checkout')}
              className="mt-5 w-full rounded-lg bg-zinc-900 py-3 text-sm font-medium text-white transition-colors hover:bg-zinc-700"
            >
              주문하기
            </button>
          </div>
        </div>
      </div>
    </main>
  )
}
