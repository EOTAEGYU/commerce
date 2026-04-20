import { serverFetch } from '@/lib/api/server'
import HeroBanner from '@/components/home/HeroBanner'
import HomeSection from '@/components/home/HomeSection'
import CategoryBanners from '@/components/home/CategoryBanners'
import type { components } from '@/types/api'

type CategoryResponse = components['schemas']['CategoryResponse']
type PageProductResponse = components['schemas']['PageProductResponse']

export default async function HomePage() {
  let newArrivals: PageProductResponse = { content: [], totalPages: 0, number: 0, size: 10 }
  let bestSellers: PageProductResponse = { content: [], totalPages: 0, number: 0, size: 10 }
  let categories: CategoryResponse[] = []

  try {
    ;[newArrivals, bestSellers, categories] = await Promise.all([
      serverFetch<PageProductResponse>('/api/products?page=0&size=10'),
      serverFetch<PageProductResponse>('/api/products?page=0&size=10'),
      serverFetch<CategoryResponse[]>('/api/categories'),
    ])
  } catch {
    // 에러 시 빈 상태로 fallback
  }

  return (
    <main className="mx-auto max-w-7xl px-4 py-6">
      {/* Hero 캐러셀 */}
      <HeroBanner />

      {/* New Arrivals */}
      <HomeSection
        title="New Arrivals"
        products={newArrivals.content ?? []}
        href="/search?sort=newest"
      />

      {/* Best Sellers */}
      <HomeSection
        title="Best Sellers · 이번 주"
        products={bestSellers.content ?? []}
        href="/search?sort=popular"
      />

      {/* Shop by Category */}
      {categories.length > 0 && (
        <section className="mb-12">
          <div className="mb-5 flex items-center justify-between border-b border-zinc-100 pb-3">
            <h2 className="text-base font-bold uppercase tracking-widest text-zinc-900">
              Shop by Category
            </h2>
          </div>
          <CategoryBanners categories={categories} />
        </section>
      )}
    </main>
  )
}
