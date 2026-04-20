'use client'

import { useState, useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'

type Props = {
  isOpen: boolean
  onClose: () => void
}

const POPULAR_SEARCHES = ['봄 자켓', '플리츠 스커트', '린넨 원피스', '트렌치 코트', '스니커즈']
const STORAGE_KEY = 'search_history'
const MAX_HISTORY = 5

function getHistory(): string[] {
  if (typeof window === 'undefined') return []
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
  } catch {
    return []
  }
}

function saveHistory(keyword: string) {
  const prev = getHistory().filter((k) => k !== keyword)
  const next = [keyword, ...prev].slice(0, MAX_HISTORY)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
}

function removeHistory(keyword: string) {
  const next = getHistory().filter((k) => k !== keyword)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
}

export default function SearchOverlay({ isOpen, onClose }: Props) {
  const router = useRouter()
  const [keyword, setKeyword] = useState('')
  const [history, setHistory] = useState<string[]>([])
  const inputRef = useRef<HTMLInputElement>(null)

  // 오버레이 열릴 때 body scroll 막기 + 기록 로드
  useEffect(() => {
    if (isOpen) {
      setHistory(getHistory())
      document.body.style.overflow = 'hidden'
      setTimeout(() => inputRef.current?.focus(), 50)
    } else {
      document.body.style.overflow = ''
      setKeyword('')
    }
    return () => {
      document.body.style.overflow = ''
    }
  }, [isOpen])

  // ESC 키 닫기
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown)
    }
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  function handleSearch(q: string) {
    const trimmed = q.trim()
    if (!trimmed) return
    saveHistory(trimmed)
    setHistory(getHistory())
    onClose()
    router.push(`/search?q=${encodeURIComponent(trimmed)}`)
  }

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    handleSearch(keyword)
  }

  function handleRemoveHistory(item: string, e: React.MouseEvent) {
    e.stopPropagation()
    removeHistory(item)
    setHistory(getHistory())
  }

  if (!isOpen) return null

  // 입력 중인 경우 suggestions 패널 표시
  const showSuggestions = keyword.trim().length > 0

  return (
    <div
      className="fixed inset-0 z-50 flex flex-col items-center pt-24 px-4"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
      onClick={onClose}
    >
      <div
        className="w-full max-w-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 검색 입력창 */}
        <form onSubmit={handleSubmit} className="relative mb-4">
          <input
            ref={inputRef}
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="검색어를 입력하세요..."
            className="w-full rounded-full border border-zinc-200 bg-white px-6 py-4 pr-14 text-sm text-zinc-900 outline-none placeholder:text-zinc-400 focus:border-zinc-900"
          />
          <button
            type="submit"
            className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-900"
            aria-label="검색"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>
        </form>

        {/* 제안 패널 (2열) */}
        <div className="grid grid-cols-2 gap-4 rounded-xl bg-white p-6 shadow-xl">
          {!showSuggestions ? (
            <>
              {/* 좌측: 최근 검색어 */}
              <div>
                <h3 className="mb-3 text-xs font-semibold uppercase tracking-widest text-zinc-400">
                  Recent
                </h3>
                {history.length === 0 ? (
                  <p className="text-sm text-zinc-400">최근 검색어가 없습니다</p>
                ) : (
                  <ul className="space-y-1">
                    {history.map((item) => (
                      <li key={item} className="flex items-center justify-between">
                        <button
                          onClick={() => handleSearch(item)}
                          className="flex items-center gap-2 text-sm text-zinc-600 hover:text-zinc-900"
                        >
                          <svg className="h-3.5 w-3.5 text-zinc-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                            <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                          </svg>
                          {item}
                        </button>
                        <button
                          onClick={(e) => handleRemoveHistory(item, e)}
                          className="text-xs text-zinc-300 hover:text-zinc-500"
                          aria-label={`${item} 삭제`}
                        >
                          ✕
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              {/* 우측: 인기 검색어 */}
              <div>
                <h3 className="mb-3 text-xs font-semibold uppercase tracking-widest text-zinc-400">
                  Popular Now
                </h3>
                <ul className="space-y-1">
                  {POPULAR_SEARCHES.map((item, i) => (
                    <li key={item}>
                      <button
                        onClick={() => handleSearch(item)}
                        className="flex items-center gap-2 text-sm text-zinc-600 hover:text-zinc-900"
                      >
                        <span className="w-4 text-xs font-bold text-zinc-300">{i + 1}</span>
                        {item}
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            </>
          ) : (
            <>
              {/* 좌측: 검색어 suggestions */}
              <div>
                <h3 className="mb-3 text-xs font-semibold uppercase tracking-widest text-zinc-400">
                  Suggestions
                </h3>
                <ul className="space-y-1">
                  {POPULAR_SEARCHES.filter((p) =>
                    p.toLowerCase().includes(keyword.toLowerCase())
                  ).map((item) => (
                    <li key={item}>
                      <button
                        onClick={() => handleSearch(item)}
                        className="text-sm text-zinc-600 hover:text-zinc-900"
                      >
                        {item}
                      </button>
                    </li>
                  ))}
                  <li>
                    <button
                      onClick={() => handleSearch(keyword)}
                      className="flex items-center gap-1 text-sm font-medium text-zinc-900"
                    >
                      <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                      </svg>
                      &quot;{keyword}&quot; 검색
                    </button>
                  </li>
                </ul>
              </div>

              {/* 우측: 최근 검색어 */}
              <div>
                <h3 className="mb-3 text-xs font-semibold uppercase tracking-widest text-zinc-400">
                  Recent
                </h3>
                {history.length === 0 ? (
                  <p className="text-sm text-zinc-400">최근 검색어가 없습니다</p>
                ) : (
                  <ul className="space-y-1">
                    {history.map((item) => (
                      <li key={item}>
                        <button
                          onClick={() => handleSearch(item)}
                          className="text-sm text-zinc-600 hover:text-zinc-900"
                        >
                          {item}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </>
          )}
        </div>

        {/* 닫기 버튼 */}
        <button
          onClick={onClose}
          className="mt-4 flex items-center gap-1 text-xs text-white/70 hover:text-white mx-auto"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
          닫기 (ESC)
        </button>
      </div>
    </div>
  )
}
