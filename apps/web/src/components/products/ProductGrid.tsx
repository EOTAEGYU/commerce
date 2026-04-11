import type { components } from '@/types/api'
import ProductCard from './ProductCard'

type ProductResponse = components['schemas']['ProductResponse']

export default function ProductGrid({ products }: { products: ProductResponse[] }) {
  if (products.length === 0) {
    return (
      <div className="py-20 text-center text-zinc-400">상품이 없습니다</div>
    )
  }

  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
      {products.map((p) => (
        <ProductCard key={p.id} product={p} />
      ))}
    </div>
  )
}
