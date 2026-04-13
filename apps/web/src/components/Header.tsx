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
  const [keyword, setKeyword] = useState('')

  const { data: categories = [] } = useQuery<CategoryResponse[]>({
    queryKey: ['categories'],
    queryFn: () => apiFetch<CategoryResponse[]>('/api/categories'),
    staleTime: 5 * 60 * 1000, // 카테고리는 자주 바뀌지 않으므로 5분
  })

  const { data: cart } = useQuery({
    queryKey: ['cart'],
    queryFn: () => apiFetch<{ items: unknown[] }>('/api/cart'),
    enabled: !!user, // 로그인 상태일 때만 요청
  })

  const cartCount = cart?.items.length ?? 0

  function handleSignOut() {
    clearAuth()
    router.push('/')
  }

  function handleSearch(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const trimmed = keyword.trim()
    if (trimmed) {
      router.push(`/?keyword=${encodeURIComponent(trimmed)}`)
    } else {
      router.push('/')
    }
  }

  return (
    <header className="sticky top-0 z-50 border-b border-zinc-200 bg-white">
      <div className="mx-auto flex max-w-7xl items-center gap-6 px-4 py-3">
        {/* 로고 */}
        <Link href="/" className="shrink-0 text-lg font-bold tracking-tight text-zinc-900">
          패션 자사몰
        </Link>

        {/* 카테고리 네비게이션 */}
        <nav className="flex gap-1">
          {categories.map((cat) => (
            <div key={cat.id} className="group relative">
              <Link
                href={`/?categoryId=${cat.id}`}
                className="rounded px-3 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-100"
              >
                {cat.name}
              </Link>
              {/* 2depth 자식 카테고리 드롭다운 */}
              {cat.children && cat.children.length > 0 && (
                <div className="absolute left-0 top-full hidden min-w-36 rounded-md border border-zinc-200 bg-white py-1 shadow-lg group-hover:block">
                  {(cat.children as CategoryResponse[]).map((child) => (
                    <Link
                      key={child.id}
                      href={`/?categoryId=${child.id}`}
                      className="block px-4 py-2 text-sm text-zinc-700 hover:bg-zinc-50"
                    >
                      {child.name}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          ))}
        </nav>

        {/* 검색창 */}
        <form onSubmit={handleSearch} className="flex items-center">
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="상품 검색"
            className="w-44 rounded-l border border-zinc-300 px-3 py-1.5 text-sm outline-none focus:border-zinc-500"
          />
          <button
            type="submit"
            className="rounded-r border border-l-0 border-zinc-300 bg-zinc-100 px-3 py-1.5 text-sm text-zinc-600 hover:bg-zinc-200"
          >
            검색
          </button>
        </form>

        {/* 우측 영역 */}
        <div className="ml-auto flex items-center gap-3">
          {/* 주문내역 */}
          {user && (
            <Link
              href="/orders"
              className="rounded px-3 py-1.5 text-sm text-zinc-700 hover:bg-zinc-100"
            >
              주문내역
            </Link>
          )}

          {/* 관리자 */}
          {user?.role === 'ADMIN' && (
            <Link
              href="/admin/products"
              className="rounded px-3 py-1.5 text-sm font-medium text-indigo-600 hover:bg-indigo-50"
            >
              관리자
            </Link>
          )}

          {/* 장바구니 */}
          <Link href="/cart" className="relative rounded p-2 hover:bg-zinc-100">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5 text-zinc-700"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={1.5}
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.25L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z"
              />
            </svg>
            {user && cartCount > 0 && (
              <span className="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-zinc-900 text-[10px] font-bold text-white">
                {cartCount > 99 ? '99+' : cartCount}
              </span>
            )}
          </Link>

          {/* 인증 버튼 */}
          {user ? (
            <>
              <Link
                href="/profile"
                className="rounded px-2 py-1 text-sm text-zinc-700 hover:bg-zinc-100"
              >
                {user.name}
              </Link>
              <button
                onClick={handleSignOut}
                className="rounded px-3 py-1.5 text-sm text-zinc-600 hover:bg-zinc-100"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link
                href="/signin"
                className="rounded px-3 py-1.5 text-sm text-zinc-700 hover:bg-zinc-100"
              >
                로그인
              </Link>
              <Link
                href="/signup"
                className="rounded bg-zinc-900 px-3 py-1.5 text-sm text-white hover:bg-zinc-700"
              >
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
