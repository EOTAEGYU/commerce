'use client'

import { useRouter, usePathname } from 'next/navigation'

type Props = {
  searchParams: Record<string, string | undefined>
}

const FILTER_LABELS: Record<string, string> = {
  size: 'Size',
  color: 'Color',
  price: 'Price',
  keyword: 'Search',
  sort: 'Sort',
}

const PRICE_LABELS: Record<string, string> = {
  under50: '5만원 미만',
  '50to100': '5만원 ~ 10만원',
  '100to200': '10만원 ~ 20만원',
  over200: '20만원 이상',
}

const FILTER_KEYS = ['size', 'color', 'price', 'keyword']

export default function AppliedFilters({ searchParams }: Props) {
  const router = useRouter()
  const pathname = usePathname()

  // 표시할 필터 칩 목록 생성
  const chips: { key: string; subValue?: string; label: string }[] = []

  for (const key of FILTER_KEYS) {
    const val = searchParams[key]
    if (!val) continue

    // size, color는 콤마 구분 멀티값
    if (key === 'size' || key === 'color') {
      val.split(',').forEach((v) => {
        chips.push({ key, subValue: v, label: `${FILTER_LABELS[key]}: ${v}` })
      })
    } else if (key === 'price') {
      chips.push({ key, label: `${FILTER_LABELS[key]}: ${PRICE_LABELS[val] ?? val}` })
    } else {
      chips.push({ key, label: `${FILTER_LABELS[key]}: ${val}` })
    }
  }

  if (chips.length === 0) return null

  function removeFilter(key: string, subValue?: string) {
    const next: Record<string, string | undefined> = { ...searchParams }

    if (subValue && (key === 'size' || key === 'color')) {
      const current = (searchParams[key] ?? '').split(',').filter((v) => v !== subValue)
      next[key] = current.length > 0 ? current.join(',') : undefined
    } else {
      next[key] = undefined
    }

    const params = new URLSearchParams()
    for (const [k, v] of Object.entries(next)) {
      if (v) params.set(k, v)
    }
    const query = params.toString()
    router.push(query ? `${pathname}?${query}` : pathname)
  }

  function clearAll() {
    const params = new URLSearchParams()
    // sort는 유지, 필터만 제거
    if (searchParams.sort) params.set('sort', searchParams.sort)
    const query = params.toString()
    router.push(query ? `${pathname}?${query}` : pathname)
  }

  return (
    <div className="mb-4 flex flex-wrap items-center gap-2">
      {chips.map((chip, i) => (
        <span
          key={i}
          className="flex items-center gap-1.5 rounded-full border border-zinc-200 bg-zinc-50 px-3 py-1 text-xs text-zinc-700"
        >
          {chip.label}
          <button
            onClick={() => removeFilter(chip.key, chip.subValue)}
            className="ml-0.5 text-zinc-400 hover:text-zinc-900"
            aria-label={`${chip.label} 필터 제거`}
          >
            ✕
          </button>
        </span>
      ))}
      <button
        onClick={clearAll}
        className="text-xs text-zinc-400 underline hover:text-zinc-700"
      >
        clear all
      </button>
    </div>
  )
}
