'use client'

import { useRouter } from 'next/navigation'

type Props = {
  totalPages: number
  currentPage: number
  categoryId?: string
  keyword?: string
  basePath?: string
}

export default function PaginationBar({ totalPages, currentPage, categoryId, keyword, basePath = '/' }: Props) {
  const router = useRouter()

  if (totalPages <= 1) return null

  function goToPage(page: number) {
    const params = new URLSearchParams()
    if (categoryId) params.set('categoryId', categoryId)
    if (keyword) params.set('keyword', keyword)
    if (page > 0) params.set('page', String(page))
    const query = params.toString()
    router.push(query ? `${basePath}?${query}` : basePath)
  }

  // 현재 페이지 기준 ±2 윈도우
  const start = Math.max(0, currentPage - 2)
  const end = Math.min(totalPages - 1, currentPage + 2)
  const pages = Array.from({ length: end - start + 1 }, (_, i) => start + i)

  return (
    <div className="flex items-center justify-center gap-1 mt-8">
      <button
        onClick={() => goToPage(currentPage - 1)}
        disabled={currentPage === 0}
        className="rounded border border-zinc-300 px-3 py-1.5 text-sm text-zinc-600 hover:bg-zinc-50 disabled:opacity-40 disabled:cursor-not-allowed"
      >
        이전
      </button>

      {start > 0 && (
        <>
          <button
            onClick={() => goToPage(0)}
            className="rounded border border-zinc-300 px-3 py-1.5 text-sm text-zinc-600 hover:bg-zinc-50"
          >
            1
          </button>
          {start > 1 && <span className="px-1 text-zinc-400">…</span>}
        </>
      )}

      {pages.map((page) => (
        <button
          key={page}
          onClick={() => goToPage(page)}
          className={`rounded border px-3 py-1.5 text-sm transition-colors ${
            page === currentPage
              ? 'bg-zinc-900 text-white border-zinc-900'
              : 'border-zinc-300 text-zinc-600 hover:bg-zinc-50'
          }`}
        >
          {page + 1}
        </button>
      ))}

      {end < totalPages - 1 && (
        <>
          {end < totalPages - 2 && <span className="px-1 text-zinc-400">…</span>}
          <button
            onClick={() => goToPage(totalPages - 1)}
            className="rounded border border-zinc-300 px-3 py-1.5 text-sm text-zinc-600 hover:bg-zinc-50"
          >
            {totalPages}
          </button>
        </>
      )}

      <button
        onClick={() => goToPage(currentPage + 1)}
        disabled={currentPage === totalPages - 1}
        className="rounded border border-zinc-300 px-3 py-1.5 text-sm text-zinc-600 hover:bg-zinc-50 disabled:opacity-40 disabled:cursor-not-allowed"
      >
        다음
      </button>
    </div>
  )
}
