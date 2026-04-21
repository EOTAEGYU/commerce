'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/auth'

const NAV_ITEMS = [
  { label: '대시보드', href: '/my' },
  { label: '주문 내역', href: '/my/orders' },
  { label: '리뷰 관리', href: '/my/reviews' },
  { label: '위시리스트', href: '/my/wishlist' },
  { label: '쿠폰함', href: '/my/coupons' },
  { label: '포인트', href: '/my/points' },
]

export default function MySidebar() {
  const pathname = usePathname()
  const router = useRouter()
  const { user, clearAuth } = useAuthStore()

  function handleLogout() {
    clearAuth()
    router.push('/')
  }

  function isActive(href: string) {
    if (href === '/my') return pathname === '/my'
    return pathname.startsWith(href)
  }

  return (
    <aside className="w-[200px] shrink-0 border border-zinc-200 rounded-lg p-4 self-start">
      {/* 유저 정보 */}
      <div className="flex items-center gap-3 mb-4">
        <div className="w-8 h-8 rounded-full bg-zinc-100 flex items-center justify-center text-sm font-semibold text-zinc-700 shrink-0">
          {user?.name?.charAt(0) ?? '?'}
        </div>
        <div className="min-w-0">
          <p className="text-sm font-semibold text-zinc-900 truncate">{user?.name ?? '-'}</p>
          <p className="text-[11px] text-zinc-400 truncate">{user?.email ?? ''}</p>
        </div>
      </div>

      <hr className="border-zinc-200 mb-3" />

      {/* 네비게이션 */}
      <nav className="flex flex-col gap-0.5">
        {NAV_ITEMS.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={
              isActive(item.href)
                ? 'bg-zinc-900 text-white rounded px-2 py-1 text-sm font-medium'
                : 'text-zinc-600 hover:bg-zinc-50 rounded px-2 py-1 text-sm'
            }
          >
            {item.label}
          </Link>
        ))}

        <hr className="border-zinc-200 my-2" />

        <button
          type="button"
          onClick={handleLogout}
          className="text-left text-zinc-600 hover:bg-zinc-50 rounded px-2 py-1 text-sm"
        >
          로그아웃
        </button>
      </nav>
    </aside>
  )
}
