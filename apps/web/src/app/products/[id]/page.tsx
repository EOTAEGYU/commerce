import { notFound } from 'next/navigation'
import type { Metadata } from 'next'
import { serverFetch } from '@/lib/api/server'
import ProductOptionPicker from '@/components/products/ProductOptionPicker'
import ProductLikeSection from '@/components/products/ProductLikeSection'
import type { components } from '@/types/api'

type ProductResponse = components['schemas']['ProductResponse']

type Props = {
  params: Promise<{ id: string }>
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params
  try {
    const product = await serverFetch<ProductResponse>(`/api/products/${id}`)
    return { title: product.name, description: product.description ?? undefined }
  } catch {
    return { title: '상품을 찾을 수 없습니다' }
  }
}

export default async function ProductDetailPage({ params }: Props) {
  const { id } = await params

  let product: ProductResponse
  try {
    product = await serverFetch<ProductResponse>(`/api/products/${id}`)
  } catch {
    notFound()
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
        {/* 상품 이미지 */}
        <div className="aspect-square rounded-xl overflow-hidden bg-zinc-100">
          {product.imageUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={product.imageUrl}
              alt={product.name ?? '상품 이미지'}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-zinc-400">
              이미지 없음
            </div>
          )}
        </div>

        {/* 상품 정보 */}
        <div className="flex flex-col gap-4">
          <div>
            <h1 className="text-2xl font-bold text-zinc-900">{product.name}</h1>
            <div className="flex items-center gap-2 mt-2">
              <p className="text-xl font-bold text-zinc-900">
                {(product.price ?? 0).toLocaleString('ko-KR')}원
              </p>
              <ProductLikeSection productId={product.id!} />
            </div>
          </div>

          {product.description && (
            <p className="text-sm text-zinc-600 leading-relaxed">{product.description}</p>
          )}

          <hr className="border-zinc-200" />

          <ProductOptionPicker
            options={product.options ?? []}
            productId={product.id!}
          />
        </div>
      </div>
    </div>
  )
}
