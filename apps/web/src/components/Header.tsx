'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import type { components } from '@/types/api'

type CategoryResponse = components['schemas']['CategoryResponse']

export default function Header() {
  const router = useRouter()
  const { user, clearAuth } = useAuthStore()
  const [searchOpen, setSearchOpen] = useState(false)
  const [keyword, setKeyword] = useState('')

  const { data: categories = [] } = useQuery<CategoryResponse[]>({
    queryKey: ['categories'],
    queryFn: () => apiFetch<CategoryResponse[]>('/api/categories'),
    staleTime: 5 * 60 * 1000,
  })

  const { data: cart } = useQuery({
    queryKey: ['cart'],
    queryFn: () => apiFetch<{ items: unknown[] }>('/api/cart'),
    enabled: !!user,
  })

  const cartCount = cart?.items.length ?? 0

  function handleSignOut() {
    clearAuth()
    router.push('/')
  }

  function handleSearch(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const trimmed = keyword.trim()
    setSearchOpen(false)
    setKeyword('')
    if (trimmed) {
      router.push(`/?keyword=${encodeURIComponent(trimmed)}`)
    } else {
      router.push('/')
    }
  }

  return (
    <header className="sticky top-0 z-50 border-b border-zinc-100 bg-white">
      <div className="mx-auto flex max-w-7xl items-center gap-4 px-4 py-2.5">

        {/* 로고 */}
        <Link href="/" className="shrink-0 text-base font-black tracking-widest text-zinc-900 uppercase">
          FASHN
        </Link>

        {/* 카테고리 GNB */}
        <nav className="flex">
          {categories.map((cat) => (
            <div key={cat.id} className="group relative">
              <Link
                href={`/?categoryId=${cat.id}`}
                className="block px-3 py-2 text-sm text-zinc-600 transition-colors hover:text-zinc-900"
              >
                {cat.name}
              </Link>
              {cat.children && cat.children.length > 0 && (
                <div className="absolute left-0 top-full hidden min-w-40 border-t-2 border-zinc-900 bg-white py-2 shadow-lg group-hover:block">
                  {(cat.children as CategoryResponse[]).map((child) => (
                    <Link
                      key={child.id}
                      href={`/?categoryId=${child.id}`}
                      className="block px-4 py-1.5 text-sm text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900"
                    >
                      {child.name}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          ))}
        </nav>

        {/* 우측 아이콘 영역 */}
        <div className="ml-auto flex items-center gap-1">

          {/* 검색 — 아이콘 클릭 시 expand */}
          {searchOpen ? (
            <form onSubmit={handleSearch} className="flex items-center">
              <input
                autoFocus
                type="text"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onBlur={() => { if (!keyword) setSearchOpen(false) }}
                placeholder="검색어를 입력하세요"
                className="w-48 border-b border-zinc-900 bg-transparent px-1 py-1 text-sm outline-none placeholder:text-zinc-400"
              />
              <button type="submit" className="p-2 text-zinc-600 hover:text-zinc-900">
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </button>
            </form>
          ) : (
            <button
              onClick={() => setSearchOpen(true)}
              className="p-2 text-zinc-500 transition-colors hover:text-zinc-900"
              aria-label="검색"
            >
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </button>
          )}

          {/* 장바구니 */}
          <Link href="/cart" className="relative p-2 text-zinc-500 transition-colors hover:text-zinc-900" aria-label="장바구니">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.25L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" />
            </svg>
            {user && cartCount > 0 && (
              <span className="absolute right-0.5 top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-zinc-900 text-[10px] font-bold text-white">
                {cartCount > 99 ? '99+' : cartCount}
              </span>
            )}
          </Link>

          <span className="mx-1 h-4 w-px bg-zinc-200" />

          {/* 인증 */}
          {user ? (
            <>
              {user.role === 'ADMIN' && (
                <Link href="/admin/products" className="px-2 py-1 text-xs font-medium text-indigo-600 hover:text-indigo-800">
                  관리자
                </Link>
              )}
              <Link href="/likes" className="px-2 py-1 text-xs text-zinc-500 hover:text-zinc-900">
                찜목록
              </Link>
              <Link href="/orders" className="px-2 py-1 text-xs text-zinc-500 hover:text-zinc-900">
                주문내역
              </Link>
              <Link href="/profile" className="px-2 py-1 text-xs text-zinc-500 hover:text-zinc-900">
                {user.name}
              </Link>
              <button onClick={handleSignOut} className="px-2 py-1 text-xs text-zinc-400 hover:text-zinc-900">
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link href="/signin" className="px-2 py-1 text-xs text-zinc-500 hover:text-zinc-900">
                로그인
              </Link>
              <Link href="/signup" className="rounded bg-zinc-900 px-3 py-1.5 text-xs font-medium text-white hover:bg-zinc-700">
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
