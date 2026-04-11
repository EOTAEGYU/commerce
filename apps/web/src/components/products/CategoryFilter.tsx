'use client'

import { useRouter } from 'next/navigation'
import type { components } from '@/types/api'

type CategoryResponse = components['schemas']['CategoryResponse']

type Props = {
  categories: CategoryResponse[]
  activeCategoryId?: string
}

export default function CategoryFilter({ categories, activeCategoryId }: Props) {
  const router = useRouter()

  function handleSelect(categoryId?: number) {
    if (categoryId === undefined) {
      router.push('/')
    } else {
      router.push(`/?categoryId=${categoryId}`)
    }
  }

  return (
    <div className="flex gap-2 overflow-x-auto pb-2 mb-6">
      <button
        onClick={() => handleSelect(undefined)}
        className={`shrink-0 rounded-full px-4 py-1.5 text-sm font-medium border transition-colors ${
          !activeCategoryId
            ? 'bg-zinc-900 text-white border-zinc-900'
            : 'bg-white text-zinc-700 border-zinc-300 hover:border-zinc-500'
        }`}
      >
        전체
      </button>
      {categories.map((cat) => (
        <button
          key={cat.id}
          onClick={() => handleSelect(cat.id)}
          className={`shrink-0 rounded-full px-4 py-1.5 text-sm font-medium border transition-colors ${
            activeCategoryId === String(cat.id)
              ? 'bg-zinc-900 text-white border-zinc-900'
              : 'bg-white text-zinc-700 border-zinc-300 hover:border-zinc-500'
          }`}
        >
          {cat.name}
        </button>
      ))}
    </div>
  )
}
