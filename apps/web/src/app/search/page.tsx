import { Suspense } from 'react'
import type { Metadata } from 'next'
import { serverFetch } from '@/lib/api/server'
import FilterSidebar from '@/components/products/FilterSidebar'
import AppliedFilters from '@/components/products/AppliedFilters'
import SortDropdown from '@/components/products/SortDropdown'
import ProductGridWithLikes from '@/components/products/ProductGridWithLikes'
import PaginationBar from '@/components/products/PaginationBar'
import type { components } from '@/types/api'

type CategoryResponse = components['schemas']['CategoryResponse']
type PageProductResponse = components['schemas']['PageProductResponse']

type Props = {
  searchParams: Promise<{
    q?: string
    page?: string
    categoryId?: string
    size?: string
    color?: string
    price?: string
    sort?: string
  }>
}

export async function generateMetadata({ searchParams }: Props): Promise<Metadata> {
  const { q } = await searchParams
  return { title: q ? `"${q}" 검색 결과` : '검색' }
}

export default async function SearchPage({ searchParams }: Props) {
  const sp = await searchParams
  const q = sp.q ?? ''
  const pageNum = Number(sp.page ?? '0')

  const productParams = new URLSearchParams({
    page: String(pageNum),
    size: '20',
  })
  if (q) productParams.set('keyword', q)
  if (sp.categoryId) productParams.set('categoryId', sp.categoryId)

  let categories: CategoryResponse[] = []
  let productsPage: PageProductResponse = { content: [], totalPages: 0, number: 0, size: 20 }

  try {
    ;[categories, productsPage] = await Promise.all([
      serverFetch<CategoryResponse[]>('/api/categories'),
      serverFetch<PageProductResponse>(`/api/products?${productParams}`),
    ])
  } catch {
    // fallback: 빈 결과
  }

  const filterParams = {
    size: sp.size,
    color: sp.color,
    price: sp.price,
    sort: sp.sort,
    keyword: q || undefined,
  }

  const appliedFilterParams = Object.fromEntries(
    Object.entries(filterParams).filter(([, v]) => v !== undefined)
  ) as Record<string, string>

  return (
    <div className="mx-auto max-w-7xl px-4 py-6">
      {/* 검색 결과 헤더 */}
      <div className="mb-6">
        {q ? (
          <h1 className="text-xl font-bold text-zinc-900">
            검색 결과: &quot;{q}&quot;
            <span className="ml-2 text-base font-normal text-zinc-400">
              {(productsPage.totalElements ?? 0).toLocaleString('ko-KR')}개
            </span>
          </h1>
        ) : (
          <h1 className="text-xl font-bold text-zinc-900">
            전체 상품
            <span className="ml-2 text-base font-normal text-zinc-400">
              {(productsPage.totalElements ?? 0).toLocaleString('ko-KR')}개
            </span>
          </h1>
        )}
      </div>

      <div className="flex gap-8">
        {/* 좌측 필터 사이드바 */}
        <FilterSidebar
          categories={categories}
          currentCategoryId={sp.categoryId ? Number(sp.categoryId) : undefined}
          searchParams={filterParams}
        />

        {/* 우측 콘텐츠 */}
        <div className="flex-1 min-w-0">
          {/* 정렬 드롭다운 */}
          <div className="mb-4 flex items-center justify-end">
            <Suspense fallback={<div className="h-9 w-28 rounded border border-zinc-200 bg-zinc-50" />}>
              <SortDropdown />
            </Suspense>
          </div>

          {/* 적용된 필터 */}
          <AppliedFilters searchParams={appliedFilterParams} />

          {/* 검색 결과 없음 */}
          {(productsPage.content ?? []).length === 0 && q && (
            <div className="py-16 text-center">
              <p className="text-zinc-400 text-sm">
                &quot;{q}&quot;에 대한 검색 결과가 없습니다.
              </p>
              <p className="mt-1 text-xs text-zinc-300">다른 검색어로 시도해 보세요.</p>
            </div>
          )}

          {/* 상품 그리드 */}
          <ProductGridWithLikes products={productsPage.content ?? []} />

          {/* 페이지네이션 */}
          <PaginationBar
            totalPages={productsPage.totalPages ?? 1}
            currentPage={productsPage.number ?? 0}
            keyword={q || undefined}
            basePath="/search"
          />
        </div>
      </div>
    </div>
  )
}
