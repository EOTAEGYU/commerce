import { serverFetch } from '@/lib/api/server'
import CategoryFilter from '@/components/products/CategoryFilter'
import ProductGrid from '@/components/products/ProductGrid'
import PaginationBar from '@/components/products/PaginationBar'
import type { components } from '@/types/api'

type CategoryResponse = components['schemas']['CategoryResponse']
type PageProductResponse = components['schemas']['PageProductResponse']

type Props = {
  searchParams: Promise<{ categoryId?: string; page?: string; keyword?: string }>
}

export default async function HomePage({ searchParams }: Props) {
  const { categoryId, page, keyword } = await searchParams
  const pageNum = Number(page ?? '0')

  const params = new URLSearchParams({ page: String(pageNum), size: '20' })
  if (categoryId) params.set('categoryId', categoryId)
  if (keyword) params.set('keyword', keyword)

  let categories: CategoryResponse[] = []
  let productsPage: PageProductResponse = { content: [], totalPages: 0, number: 0, size: 20 }

  try {
    ;[categories, productsPage] = await Promise.all([
      serverFetch<CategoryResponse[]>('/api/categories'),
      serverFetch<PageProductResponse>(`/api/products?${params}`),
    ])
  } catch {
    return (
      <div className="mx-auto max-w-7xl px-4 py-8">
        <p className="text-center text-zinc-400">상품을 불러오는 중 오류가 발생했습니다.</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      {keyword && (
        <p className="mb-4 text-sm text-zinc-500">
          <span className="font-medium text-zinc-800">&ldquo;{keyword}&rdquo;</span> 검색 결과
        </p>
      )}
      <CategoryFilter categories={categories} activeCategoryId={categoryId} keyword={keyword} />
      <ProductGrid products={productsPage.content ?? []} />
      <PaginationBar
        totalPages={productsPage.totalPages ?? 1}
        currentPage={productsPage.number ?? 0}
        categoryId={categoryId}
        keyword={keyword}
      />
    </div>
  )
}
