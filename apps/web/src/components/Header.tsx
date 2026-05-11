'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import SearchOverlay from '@/components/search/SearchOverlay'
import type { components } from '@/types/api'

type CategoryResponse = components['schemas']['CategoryResponse']

export default function Header() {
  const router = useRouter()
  const { user, clearAuth } = useAuthStore()
  const [isSearchOpen, setIsSearchOpen] = useState(false)

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

  return (
    <>
      <SearchOverlay isOpen={isSearchOpen} onClose={() => setIsSearchOpen(false)} />
      <header className="sticky top-0 z-40 border-b border-zinc-100 bg-white">
        {/* Row 1: 상단바 */}
        <div className="mx-auto grid max-w-7xl grid-cols-3 items-center px-4 py-3">
          {/* 좌: 검색바 버튼 */}
          <div>
            <button
              onClick={() => setIsSearchOpen(true)}
              className="flex items-center gap-2 rounded border border-zinc-200 px-3 py-1.5 text-sm text-zinc-400 hover:border-zinc-400 transition-colors w-56"
            >
              <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <span>검색어를 입력하세요</span>
            </button>
          </div>

          {/* 중: 로고 */}
          <div className="flex justify-center">
            <Link href="/" className="text-xl font-black tracking-widest text-zinc-900 uppercase">
              FASHN
            </Link>
          </div>

          {/* 우: 인증 링크 */}
          <div className="flex items-center justify-end gap-3">
            {/* 장바구니 */}
            <Link href="/cart" className="relative text-zinc-600 hover:text-zinc-900" aria-label="장바구니">
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.25L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" />
              </svg>
              {user && cartCount > 0 && (
                <span className="absolute -right-1.5 -top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-zinc-900 text-[10px] font-bold text-white">
                  {cartCount > 99 ? '99+' : cartCount}
                </span>
              )}
            </Link>

            <span className="h-4 w-px bg-zinc-200" />

            {/* 로그인 상태별 링크 */}
            {user ? (
              <>
                {user.role === 'ADMIN' && (
                  <Link href="/admin/products" className="text-xs font-medium text-indigo-600 hover:text-indigo-800">
                    관리자
                  </Link>
                )}
                <Link href="/my/orders" className="text-xs text-zinc-600 hover:text-zinc-900">
                  주문조회
                </Link>
                <Link href="/my" className="text-xs text-zinc-600 hover:text-zinc-900">
                  마이페이지
                </Link>
                <button onClick={handleSignOut} className="text-xs text-zinc-400 hover:text-zinc-900">
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <Link href="/my/orders" className="text-xs text-zinc-600 hover:text-zinc-900">
                  주문조회
                </Link>
                <Link href="/my" className="text-xs text-zinc-600 hover:text-zinc-900">
                  마이페이지
                </Link>
                <Link href="/signin" className="text-xs text-zinc-600 hover:text-zinc-900">
                  로그인
                </Link>
                <Link href="/signup" className="text-xs text-zinc-600 hover:text-zinc-900">
                  회원가입
                </Link>
              </>
            )}
          </div>
        </div>

        {/* Row 2: 카테고리바 */}
        <nav className="border-t border-zinc-100">
          <div className="mx-auto flex max-w-7xl justify-center px-4">
            {categories.map((cat) => (
              <div key={cat.id} className="group relative">
                <Link
                  href={`/categories/${cat.id}`}
                  className="block px-4 py-2.5 text-sm font-medium text-zinc-700 transition-colors hover:text-zinc-900"
                >
                  {cat.name}
                </Link>
                {cat.children && cat.children.length > 0 && (
                  <div className="absolute left-0 top-full z-50 hidden min-w-40 border-t-2 border-zinc-900 bg-white py-2 shadow-lg group-hover:block">
                    {(cat.children as CategoryResponse[]).map((child) => (
                      <Link
                        key={child.id}
                        href={`/categories/${child.id}`}
                        className="block px-4 py-1.5 text-sm text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900"
                      >
                        {child.name}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        </nav>
      </header>
    </>
  )
}
