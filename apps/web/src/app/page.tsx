import { serverFetch } from '@/lib/api/server'
import CategoryFilter from '@/components/products/CategoryFilter'
import ProductGridWithLikes from '@/components/products/ProductGridWithLikes'
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

  const total = productsPage.totalElements ?? 0

  return (
    <div className="mx-auto max-w-7xl px-4 py-6">
      {/* 프로모션 배너 */}
      {!keyword && !categoryId && (
        <div className="mb-6 flex items-center justify-center bg-zinc-950 py-3 text-sm text-white">
          <span className="mr-2">🔥</span>
          <span>신규 회원 첫 구매 10% 할인 &mdash; 지금 바로 쇼핑하세요</span>
          <span className="ml-2">🔥</span>
        </div>
      )}

      {/* 타이틀 + 검색 결과 */}
      <div className="mb-2 flex items-baseline justify-between">
        <h1 className="text-base font-bold text-zinc-900">
          {keyword
            ? `"${keyword}" 검색 결과`
            : categoryId
              ? categories.find(c => String(c.id) === categoryId)?.name ?? '상품'
              : '전체 상품'}
          <span className="ml-2 text-sm font-normal text-zinc-400">({total.toLocaleString('ko-KR')})</span>
        </h1>
      </div>

      <CategoryFilter categories={categories} activeCategoryId={categoryId} keyword={keyword} />
      <ProductGridWithLikes products={productsPage.content ?? []} />
      <PaginationBar
        totalPages={productsPage.totalPages ?? 1}
        currentPage={productsPage.number ?? 0}
        categoryId={categoryId}
        keyword={keyword}
      />
    </div>
  )
}
