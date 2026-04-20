import Link from 'next/link'
import ProductCard from '@/components/products/ProductCard'
import type { components } from '@/types/api'

type ProductResponse = components['schemas']['ProductResponse']

type Props = {
  title: string
  products: ProductResponse[]
  href: string
}

export default function HomeSection({ title, products, href }: Props) {
  if (products.length === 0) return null

  return (
    <section className="mb-12">
      {/* 섹션 헤더 */}
      <div className="mb-5 flex items-center justify-between border-b border-zinc-100 pb-3">
        <h2 className="text-base font-bold uppercase tracking-widest text-zinc-900">{title}</h2>
        <Link
          href={href}
          className="flex items-center gap-1 text-xs font-medium uppercase tracking-widest text-zinc-400 transition-colors hover:text-zinc-900"
        >
          See All
          <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
          </svg>
        </Link>
      </div>

      {/* 5열 상품 그리드 */}
      <div className="grid grid-cols-5 gap-4">
        {products.slice(0, 5).map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
    </section>
  )
}
