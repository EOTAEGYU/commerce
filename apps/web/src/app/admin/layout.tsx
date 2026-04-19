'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import Link from 'next/link'
import { useAuthStore } from '@/store/auth'

const NAV_ITEMS = [
  { href: '/admin/products', label: '상품 관리' },
  { href: '/admin/categories', label: '카테고리 관리' },
]

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const pathname = usePathname()
  const { user } = useAuthStore()
  const [hydrated, setHydrated] = useState(false)

  useEffect(() => {
    useAuthStore.persist.rehydrate()
    setHydrated(true)
  }, [])

  if (!hydrated) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <p className="text-sm text-zinc-400">로딩 중...</p>
      </div>
    )
  }

  if (!user) {
    router.replace('/signin')
    return null
  }

  if (user.role !== 'ADMIN') {
    router.replace('/')
    return null
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      <div className="flex gap-8">
        {/* 사이드바 */}
        <aside className="w-48 shrink-0">
          <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-zinc-400">
            관리자
          </p>
          <nav className="space-y-1">
            {NAV_ITEMS.map(({ href, label }) => (
              <Link
                key={href}
                href={href}
                className={`block rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                  pathname.startsWith(href)
                    ? 'bg-zinc-900 text-white'
                    : 'text-zinc-700 hover:bg-zinc-100'
                }`}
              >
                {label}
              </Link>
            ))}
          </nav>
        </aside>

        {/* 본문 */}
        <div className="min-w-0 flex-1">{children}</div>
      </div>
    </div>
  )
}
