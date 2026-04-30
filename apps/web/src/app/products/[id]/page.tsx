import { notFound } from 'next/navigation'
import type { Metadata } from 'next'
import { serverFetch } from '@/lib/api/server'
import Breadcrumb from '@/components/layout/Breadcrumb'
import ProductOptionPicker from '@/components/products/ProductOptionPicker'
import ProductLikeSection from '@/components/products/ProductLikeSection'
import Accordion from '@/components/products/Accordion'
import type { components } from '@/types/api'

type ProductResponse = components['schemas']['ProductResponse'] & {
  averageRating?: number
  reviewCount?: number
}

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

  const breadcrumbItems = [
    { label: 'Home', href: '/' },
    { label: 'Products' },
  ]

  return (
    <div className="mx-auto max-w-7xl px-4 py-6">
      <Breadcrumb items={breadcrumbItems} />

      <div className="grid gap-10" style={{ gridTemplateColumns: '1.15fr 1fr' }}>
        {/* 좌측 갤러리 */}
        <div>
          <div className="grid grid-cols-2 gap-2">
            {/* 메인 이미지 (col-span-2) */}
            <div className="relative col-span-2 overflow-hidden bg-zinc-100" style={{ aspectRatio: '3/3.2' }}>
              {product.imageUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={product.imageUrl}
                  alt={product.name ?? '상품 이미지'}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="flex h-full w-full items-center justify-center">
                  <svg className="h-16 w-16 text-zinc-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                </div>
              )}
            </div>

            {/* 서브 이미지 4개 (placeholder) */}
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="overflow-hidden bg-zinc-100"
                style={{ aspectRatio: '3/4' }}
              >
                <div className="flex h-full w-full items-center justify-center bg-zinc-50">
                  <svg className="h-8 w-8 text-zinc-200" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 우측 sticky 패널 */}
        <div className="sticky top-20 self-start">
          {/* 브랜드 */}
          <p className="text-xs font-mono uppercase tracking-widest text-zinc-400">FASHN BRAND</p>

          {/* 상품명 */}
          <h1 className="mt-1 text-3xl font-bold text-zinc-900 leading-tight">
            {product.name}
          </h1>

          {/* 평점 + 리뷰 + 좋아요 */}
          <div className="mt-2 flex items-center gap-2 text-sm text-zinc-400">
            {(product.averageRating ?? 0) > 0 && (
              <>
                <span className="text-yellow-400">★</span>
                <span>{(product.averageRating ?? 0).toFixed(1)}</span>
                <span>·</span>
              </>
            )}
            <span>{(product.reviewCount ?? 0).toLocaleString('ko-KR')}개 리뷰</span>
            <span>·</span>
            <ProductLikeSection productId={product.id!} />
          </div>

          {/* 가격 */}
          <p className="mt-4 text-3xl font-bold text-zinc-900">
            {(product.price ?? 0).toLocaleString('ko-KR')}원
          </p>

          <hr className="my-5 border-zinc-100" />

          {/* 옵션 선택 (색상/사이즈 + 장바구니 버튼 포함) */}
          <ProductOptionPicker
            options={product.options ?? []}
            productId={product.id!}
          />

          {/* 배송 안내 */}
          <ul className="mt-5 space-y-1.5 text-sm text-zinc-400">
            <li>· 5만원 이상 무료배송</li>
            <li>· 평일 14시 이전 주문 당일 출고</li>
            <li>· 30일 무료 반품</li>
          </ul>

          <hr className="my-5 border-zinc-100" />

          {/* 상품 설명 */}
          {product.description && (
            <p className="mb-4 text-sm text-zinc-500 leading-relaxed">{product.description}</p>
          )}

          {/* 아코디언 */}
          <Accordion
            title="Details"
            content="고품질 소재로 제작된 제품입니다. 세탁 시 찬물 손세탁을 권장하며, 직사광선을 피해 보관해 주세요."
          />
          <Accordion
            title="Size & Fit"
            content="모델 신장 175cm, 사이즈 M 착용. 일반적인 체형에 맞게 제작되었습니다. 사이즈가 걱정되시면 한 치수 크게 선택하시는 것을 권장합니다."
          />
          <Accordion
            title="Shipping & Returns"
            content="5만원 이상 무료배송. 평일 오후 2시 이전 주문 시 당일 출고됩니다. 수령 후 30일 이내 미착용 상태에서 반품 가능합니다."
          />
        </div>
      </div>
    </div>
  )
}
