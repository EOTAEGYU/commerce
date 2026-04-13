'use client'

import { useRouter } from 'next/navigation'
import type { components } from '@/types/api'

type CategoryResponse = components['schemas']['CategoryResponse']

type Props = {
  categories: CategoryResponse[]
  activeCategoryId?: string
  keyword?: string
}

export default function CategoryFilter({ categories, activeCategoryId, keyword }: Props) {
  const router = useRouter()

  function handleSelect(categoryId?: number) {
    const params = new URLSearchParams()
    if (categoryId !== undefined) params.set('categoryId', String(categoryId))
    if (keyword) params.set('keyword', keyword)
    const query = params.toString()
    router.push(query ? `/?${query}` : '/')
  }

  return (
    <div className="flex overflow-x-auto border-b border-zinc-100 mb-6">
      <button
        onClick={() => handleSelect(undefined)}
        className={`shrink-0 px-4 py-2.5 text-sm transition-colors ${
          !activeCategoryId
            ? 'border-b-2 border-zinc-900 font-bold text-zinc-900'
            : 'text-zinc-400 hover:text-zinc-900'
        }`}
      >
        전체
      </button>
      {categories.map((cat) => (
        <button
          key={cat.id}
          onClick={() => handleSelect(cat.id)}
          className={`shrink-0 px-4 py-2.5 text-sm transition-colors ${
            activeCategoryId === String(cat.id)
              ? 'border-b-2 border-zinc-900 font-bold text-zinc-900'
              : 'text-zinc-400 hover:text-zinc-900'
          }`}
        >
          {cat.name}
        </button>
      ))}
    </div>
  )
}
