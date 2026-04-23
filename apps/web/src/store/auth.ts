import { create } from 'zustand'
import { persist } from 'zustand/middleware'

// 백엔드 UserResponse 기준 (id는 Long → number로 매핑)
type AuthUser = {
  id: number
  email: string
  name: string
  role: 'USER' | 'ADMIN'
  username?: string  // 소셜 로그인 계정은 없을 수 있어 optional
}

type AuthStore = {
  token: string | null
  user: AuthUser | null
  setAuth: (token: string, user: AuthUser) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setAuth: (token, user) => set({ token, user }),
      clearAuth: () => set({ token: null, user: null }),
    }),
    {
      name: 'auth-storage', // localStorage 키 이름
      // SSR에서는 localStorage가 없으므로 hydration mismatch 방지
      // 클라이언트 컴포넌트에서 useEffect로 수동 rehydrate 필요
      skipHydration: true,
    }
  )
)
