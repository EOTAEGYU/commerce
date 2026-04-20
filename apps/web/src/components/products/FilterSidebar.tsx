'use client'

import Link from 'next/link'
import { useRouter, usePathname } from 'next/navigation'
import type { components } from '@/types/api'

type CategoryResponse = components['schemas']['CategoryResponse']

type FilterParams = {
  size?: string
  color?: string
  price?: string
  sort?: string
}

type Props = {
  categories: CategoryResponse[]
  currentCategoryId?: number
  searchParams: FilterParams
}

const SIZES = ['XS', 'S', 'M', 'L', 'XL']

const COLORS: { label: string; value: string; hex: string }[] = [
  { label: 'Black', value: 'black', hex: '#18181b' },
  { label: 'White', value: 'white', hex: '#fafafa' },
  { label: 'Gray', value: 'gray', hex: '#a1a1aa' },
  { label: 'Beige', value: 'beige', hex: '#d4c5a9' },
  { label: 'Navy', value: 'navy', hex: '#1e3a5f' },
  { label: 'Red', value: 'red', hex: '#ef4444' },
]

const PRICE_RANGES = [
  { label: '5만원 미만', value: 'under50' },
  { label: '5만원 ~ 10만원', value: '50to100' },
  { label: '10만원 ~ 20만원', value: '100to200' },
  { label: '20만원 이상', value: 'over200' },
]

export default function FilterSidebar({ categories, currentCategoryId, searchParams }: Props) {
  const router = useRouter()
  const pathname = usePathname()

  const selectedSizes = searchParams.size ? searchParams.size.split(',') : []
  const selectedColors = searchParams.color ? searchParams.color.split(',') : []
  const selectedPrice = searchParams.price ?? ''

  function buildQuery(overrides: Record<string, string | undefined>) {
    const params = new URLSearchParams()
    const merged = { ...searchParams, ...overrides }
    if (merged.size) params.set('size', merged.size)
    if (merged.color) params.set('color', merged.color)
    if (merged.price) params.set('price', merged.price)
    if (merged.sort) params.set('sort', merged.sort)
    return params.toString()
  }

  function toggleMulti(current: string[], value: string): string | undefined {
    const next = current.includes(value)
      ? current.filter((v) => v !== value)
      : [...current, value]
    return next.length > 0 ? next.join(',') : undefined
  }

  function handleSize(size: string) {
    const next = toggleMulti(selectedSizes, size)
    const query = buildQuery({ size: next })
    router.push(query ? `${pathname}?${query}` : pathname)
  }

  function handleColor(color: string) {
    const next = toggleMulti(selectedColors, color)
    const query = buildQuery({ color: next })
    router.push(query ? `${pathname}?${query}` : pathname)
  }

  function handlePrice(price: string) {
    const next = selectedPrice === price ? undefined : price
    const query = buildQuery({ price: next })
    router.push(query ? `${pathname}?${query}` : pathname)
  }

  // 상위 카테고리 목록 (children 포함)
  const topCategories = categories

  return (
    <aside className="w-[200px] shrink-0">
      {/* Category */}
      <div className="mb-6">
        <h3 className="mb-3 text-xs font-semibold uppercase tracking-widest text-zinc-400">
          Category
        </h3>
        <ul className="space-y-0.5">
          {topCategories.map((cat) => (
            <li key={cat.id}>
              <Link
                href={`/categories/${cat.id}`}
                className={`block rounded px-2 py-1.5 text-sm transition-colors ${
                  currentCategoryId === cat.id
                    ? 'bg-zinc-900 text-white font-medium'
                    : 'text-zinc-600 hover:text-zinc-900 hover:bg-zinc-50'
                }`}
              >
                {cat.name}
              </Link>
              {cat.children && cat.children.length > 0 && (
                <ul className="mt-0.5 ml-3 space-y-0.5">
                  {(cat.children as CategoryResponse[]).map((child) => (
                    <li key={child.id}>
                      <Link
                        href={`/categories/${child.id}`}
                        className={`block rounded px-2 py-1.5 text-sm transition-colors ${
                          currentCategoryId === child.id
                            ? 'bg-zinc-900 text-white font-medium'
                            : 'text-zinc-500 hover:text-zinc-900 hover:bg-zinc-50'
                        }`}
                      >
                        {child.name}
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ul>
      </div>

      <hr className="mb-6 border-zinc-100" />

      {/* Size */}
      <div className="mb-6">
        <h3 className="mb-3 text-xs font-semibold uppercase tracking-widest text-zinc-400">Size</h3>
        <div className="flex flex-wrap gap-1.5">
          {SIZES.map((size) => {
            const active = selectedSizes.includes(size)
            return (
              <button
                key={size}
                onClick={() => handleSize(size)}
                className={`rounded border px-2.5 py-1 text-xs font-medium transition-colors ${
                  active
                    ? 'bg-zinc-900 text-white border-zinc-900'
                    : 'border-zinc-200 text-zinc-600 hover:border-zinc-500'
                }`}
              >
                {size}
              </button>
            )
          })}
        </div>
      </div>

      <hr className="mb-6 border-zinc-100" />

      {/* Color */}
      <div className="mb-6">
        <h3 className="mb-3 text-xs font-semibold uppercase tracking-widest text-zinc-400">Color</h3>
        <div className="flex flex-wrap gap-2">
          {COLORS.map(({ label, value, hex }) => {
            const active = selectedColors.includes(value)
            return (
              <button
                key={value}
                onClick={() => handleColor(value)}
                title={label}
                className={`relative h-[18px] w-[18px] rounded-full border transition-all ${
                  active ? 'scale-110 ring-2 ring-zinc-900 ring-offset-1' : 'border-zinc-200 hover:scale-110'
                }`}
                style={{ backgroundColor: hex }}
                aria-label={label}
              />
            )
          })}
        </div>
      </div>

      <hr className="mb-6 border-zinc-100" />

      {/* Price */}
      <div className="mb-6">
        <h3 className="mb-3 text-xs font-semibold uppercase tracking-widest text-zinc-400">Price</h3>
        <ul className="space-y-1.5">
          {PRICE_RANGES.map(({ label, value }) => (
            <li key={value}>
              <label className="flex cursor-pointer items-center gap-2 text-sm text-zinc-600 hover:text-zinc-900">
                <input
                  type="checkbox"
                  checked={selectedPrice === value}
                  onChange={() => handlePrice(value)}
                  className="h-3.5 w-3.5 rounded accent-zinc-900"
                />
                {label}
              </label>
            </li>
          ))}
        </ul>
      </div>
    </aside>
  )
}
