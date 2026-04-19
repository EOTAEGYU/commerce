'use client'

import { useEffect } from 'react'
import QueryProvider from '@/providers/QueryProvider'
import { useAuthStore } from '@/store/auth'

// skipHydration: true로 설정된 Zustand persist 스토어는
// 클라이언트 마운트 시 수동으로 rehydrate 해야 localStorage 값을 읽어온다.
function AuthHydration() {
  useEffect(() => {
    useAuthStore.persist.rehydrate()
  }, [])
  return null
}

// 추후 추가될 Provider들(Toast, Theme 등)을 이 파일에서 조합
export default function Providers({ children }: { children: React.ReactNode }) {
  return (
    <QueryProvider>
      <AuthHydration />
      {children}
    </QueryProvider>
  )
}
