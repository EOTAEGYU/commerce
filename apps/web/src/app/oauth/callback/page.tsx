'use client'

import { Suspense, useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'

function OAuthCallbackInner() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const setAuth = useAuthStore((s) => s.setAuth)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    const token = searchParams.get('token')
    const error = searchParams.get('error')

    if (error) {
      setErrorMessage('소셜 로그인에 실패했습니다. 잠시 후 로그인 페이지로 이동합니다.')
      const timer = setTimeout(() => {
        router.replace('/signin')
      }, 2000)
      return () => clearTimeout(timer)
    }

    if (!token) {
      router.replace('/signin')
      return
    }

    async function processLogin() {
      try {
        const user = await apiFetch<{ id: number; email: string; name: string; role: 'USER' | 'ADMIN' }>(
          '/api/users/me',
          { headers: { Authorization: `Bearer ${token}` } }
        )
        setAuth(token!, user)
        router.push('/')
      } catch (err) {
        if (err instanceof ApiError && err.code === 'UNAUTHORIZED') {
          router.replace('/signin')
        } else {
          router.replace('/signin')
        }
      }
    }

    processLogin()
  }, [searchParams, router, setAuth])

  if (errorMessage) {
    return (
      <div className="flex min-h-screen items-center justify-center px-4">
        <div className="text-center">
          <p className="text-sm text-red-500">{errorMessage}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="text-center">
        <p className="text-sm text-zinc-500">로그인 처리 중...</p>
      </div>
    </div>
  )
}

export default function OAuthCallbackPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center px-4">
          <div className="text-center">
            <p className="text-sm text-zinc-500">로그인 처리 중...</p>
          </div>
        </div>
      }
    >
      <OAuthCallbackInner />
    </Suspense>
  )
}
