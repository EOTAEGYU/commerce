import { Suspense } from 'react'
import { notFound } from 'next/navigation'
import type { Metadata } from 'next'
import { serverFetch } from '@/lib/api/server'
import Breadcrumb from '@/components/layout/Breadcrumb'
import FilterSidebar from '@/components/products/FilterSidebar'
import AppliedFilters from '@/components/products/AppliedFilters'
import SortDropdown from '@/components/products/SortDropdown'
import ProductGridWithLikes from '@/components/products/ProductGridWithLikes'
import PaginationBar from '@/components/products/PaginationBar'
import type { components } from '@/types/api'

type CategoryResponse = components['schemas']['CategoryResponse']
type PageProductResponse = components['schemas']['PageProductResponse']

type Props = {
  params: Promise<{ id: string }>
  searchParams: Promise<{
    page?: string
    size?: string
    color?: string
    price?: string
    sort?: string
  }>
}

function findCategoryById(
  categories: CategoryResponse[],
  id: number
): CategoryResponse | undefined {
  for (const cat of categories) {
    if (cat.id === id) return cat
    if (cat.children) {
      const found = findCategoryById(cat.children as CategoryResponse[], id)
      if (found) return found
    }
  }
  return undefined
}

function findParentCategory(
  categories: CategoryResponse[],
  childId: number
): CategoryResponse | undefined {
  for (const cat of categories) {
    if (cat.children?.some((c) => c.id === childId)) return cat
  }
  return undefined
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params
  try {
    const categories = await serverFetch<CategoryResponse[]>('/api/categories')
    const cat = findCategoryById(categories, Number(id))
    return { title: cat?.name ?? '카테고리' }
  } catch {
    return { title: '카테고리' }
  }
}

export default async function CategoryPage({ params, searchParams }: Props) {
  const { id } = await params
  const sp = await searchParams
  const pageNum = Number(sp.page ?? '0')
  const categoryId = Number(id)

  const productParams = new URLSearchParams({
    categoryId: id,
    page: String(pageNum),
    size: '20',
  })

  let categories: CategoryResponse[] = []
  let productsPage: PageProductResponse = { content: [], totalPages: 0, number: 0, size: 20 }

  try {
    ;[categories, productsPage] = await Promise.all([
      serverFetch<CategoryResponse[]>('/api/categories'),
      serverFetch<PageProductResponse>(`/api/products?${productParams}`),
    ])
  } catch {
    notFound()
  }

  const currentCategory = findCategoryById(categories, categoryId)
  const parentCategory = findParentCategory(categories, categoryId)

  if (!currentCategory) notFound()

  // 브레드크럼 아이템 구성
  const breadcrumbItems = [
    { label: 'Home', href: '/' },
    ...(parentCategory
      ? [{ label: parentCategory.name ?? '', href: `/categories/${parentCategory.id}` }]
      : []),
    { label: currentCategory.name ?? '' },
  ]

  const filterParams = {
    size: sp.size,
    color: sp.color,
    price: sp.price,
    sort: sp.sort,
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-6">
      <Breadcrumb items={breadcrumbItems} />

      <div className="flex gap-8">
        {/* 좌측 필터 사이드바 */}
        <FilterSidebar
          categories={categories}
          currentCategoryId={categoryId}
          searchParams={filterParams}
        />

        {/* 우측 콘텐츠 */}
        <div className="flex-1 min-w-0">
          {/* 헤더: 제목 + 정렬 */}
          <div className="mb-4 flex items-center justify-between">
            <h1 className="text-lg font-bold text-zinc-900">
              {currentCategory.name}
              <span className="ml-2 text-sm font-normal text-zinc-400">
                {(productsPage.totalElements ?? 0).toLocaleString('ko-KR')}개
              </span>
            </h1>
            <Suspense fallback={<div className="h-9 w-28 rounded border border-zinc-200 bg-zinc-50" />}>
              <SortDropdown />
            </Suspense>
          </div>

          {/* 적용된 필터 */}
          <AppliedFilters
            searchParams={Object.fromEntries(
              Object.entries(filterParams).filter(([, v]) => v !== undefined)
            ) as Record<string, string>}
          />

          {/* 상품 그리드 */}
          <ProductGridWithLikes products={productsPage.content ?? []} />

          {/* 페이지네이션 */}
          <PaginationBar
            totalPages={productsPage.totalPages ?? 1}
            currentPage={productsPage.number ?? 0}
            basePath={`/categories/${id}`}
          />
        </div>
      </div>
    </div>
  )
}
