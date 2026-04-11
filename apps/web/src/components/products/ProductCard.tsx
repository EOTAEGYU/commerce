import Link from 'next/link'
import type { components } from '@/types/api'

type ProductResponse = components['schemas']['ProductResponse']

export default function ProductCard({ product }: { product: ProductResponse }) {
  return (
    <Link href={`/products/${product.id}`}>
      <article className="group rounded-lg border border-zinc-200 overflow-hidden hover:shadow-md transition-shadow">
        <div className="aspect-square bg-zinc-100 overflow-hidden">
          {product.imageUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={product.imageUrl}
              alt={product.name ?? '상품 이미지'}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-zinc-400 text-sm">
              이미지 없음
            </div>
          )}
        </div>
        <div className="p-3">
          <h2 className="text-sm font-medium text-zinc-900 line-clamp-2">{product.name}</h2>
          <p className="text-sm font-bold text-zinc-900 mt-1">
            {(product.price ?? 0).toLocaleString('ko-KR')}원
          </p>
        </div>
      </article>
    </Link>
  )
}
