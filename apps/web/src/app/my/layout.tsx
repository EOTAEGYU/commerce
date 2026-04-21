'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/auth'
import MySidebar from '@/components/my/MySidebar'

export default function MyLayout({ children }: { children: React.ReactNode }) {
  const { user } = useAuthStore()
  const router = useRouter()

  useEffect(() => {
    if (!user) {
      router.replace('/signin?redirect=/my')
    }
  }, [user, router])

  if (!user) return null

  return (
    <div className="max-w-5xl mx-auto px-4 py-10 flex gap-8">
      <MySidebar />
      <main className="flex-1 min-w-0">{children}</main>
    </div>
  )
}
