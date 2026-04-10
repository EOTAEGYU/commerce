'use client'

import QueryProvider from '@/providers/QueryProvider'

// 추후 추가될 Provider들(Toast, Theme 등)을 이 파일에서 조합
export default function Providers({ children }: { children: React.ReactNode }) {
  return <QueryProvider>{children}</QueryProvider>
}
