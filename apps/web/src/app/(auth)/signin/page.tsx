'use client'

import { Suspense, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'

function SignInInner() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const setAuth = useAuthStore((s) => s.setAuth)

  const handleKakaoLogin = () => {
    window.location.href = `${process.env.NEXT_PUBLIC_API_URL}/oauth2/authorization/kakao`
  }

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [secureLogin, setSecureLogin] = useState(true)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const { accessToken } = await apiFetch<{ accessToken: string; tokenType: string }>(
        '/api/users/signin',
        { method: 'POST', body: JSON.stringify({ username, password }) }
      )

      const user = await apiFetch<{ id: number; email: string; name: string; role: 'USER' | 'ADMIN'; username?: string }>(
        '/api/users/me',
        { headers: { Authorization: `Bearer ${accessToken}` } }
      )

      setAuth(accessToken, user)

      const redirectUrl = searchParams.get('redirect') ?? '/'
      router.push(redirectUrl)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(
          err.code === 'INVALID_CREDENTIALS'
            ? '아이디 또는 비밀번호가 올바르지 않습니다.'
            : err.message
        )
      } else {
        setError('로그인 중 오류가 발생했습니다.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="py-16 px-4">
      <div className="mx-auto w-full max-w-[340px]">
        {/* 타이틀 */}
        <h1 className="mb-6 text-center text-2xl font-bold text-zinc-900">로그인</h1>

        {/* 부제 */}
        <p className="mb-8 text-center text-sm leading-relaxed text-zinc-400">
          아이디와 비밀번호 입력 없이<br />
          1초 회원가입으로 간편하게 로그인하세요.
        </p>

        {/* 카카오 버튼 */}
        <button
          type="button"
          onClick={handleKakaoLogin}
          className="mb-6 flex w-full items-center justify-center gap-2 rounded bg-[#FEE500] py-3.5 text-sm font-medium text-zinc-900"
        >
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M9 1C4.582 1 1 3.79 1 7.23c0 2.2 1.456 4.133 3.65 5.238L3.72 16.07a.25.25 0 0 0 .38.27L8.37 13.4c.21.02.422.03.63.03 4.418 0 8-2.79 8-6.23C17 3.79 13.418 1 9 1Z"
              fill="currentColor"
            />
          </svg>
          카카오 1초 로그인·회원가입
        </button>

        {/* 구분선 */}
        <div className="mb-6 flex items-center gap-3">
          <span className="flex-1 border-t border-zinc-200" />
          <span className="text-xs text-zinc-400">또는</span>
          <span className="flex-1 border-t border-zinc-200" />
        </div>

        {/* 폼 */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <input
            type="text"
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="아이디"
            className="w-full border border-zinc-200 px-4 py-3 text-sm text-zinc-800 outline-none focus:border-zinc-500"
          />
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="패스워드"
            className="w-full border border-zinc-200 px-4 py-3 text-sm text-zinc-800 outline-none focus:border-zinc-500"
          />

          {/* 체크박스 */}
          <div className="flex items-center gap-4">
            <label className="flex cursor-pointer items-center gap-1.5">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                className="accent-zinc-900"
              />
              <span className="text-xs text-zinc-500">아이디 저장</span>
            </label>
            <label className="flex cursor-pointer items-center gap-1.5">
              <svg width="12" height="14" viewBox="0 0 12 14" fill="none" className="text-zinc-400">
                <rect x="1" y="6" width="10" height="7" rx="1" stroke="currentColor" strokeWidth="1.2" />
                <path d="M3.5 6V4a2.5 2.5 0 0 1 5 0v2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
              </svg>
              <input
                type="checkbox"
                checked={secureLogin}
                onChange={(e) => setSecureLogin(e.target.checked)}
                className="hidden"
              />
              <span className="text-xs text-zinc-500">보안접속</span>
            </label>
          </div>

          {/* 에러 */}
          {error && (
            <p className="rounded bg-red-50 px-3 py-2 text-xs text-red-500">{error}</p>
          )}

          {/* 로그인 버튼 */}
          <button
            type="submit"
            disabled={loading}
            className="mt-1 w-full bg-zinc-900 py-3.5 text-sm font-medium text-white disabled:opacity-50"
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        {/* 하단 링크 */}
        <div className="mt-5 flex items-center justify-center gap-0 text-xs text-zinc-400">
          <a href="#" className="px-3 hover:text-zinc-600">아이디찾기</a>
          <span>|</span>
          <a href="#" className="px-3 hover:text-zinc-600">비밀번호찾기</a>
          <span>|</span>
          <Link href="/signup" className="px-3 hover:text-zinc-600">회원가입</Link>
        </div>
      </div>
    </div>
  )
}

export default function SignInPage() {
  return (
    <Suspense fallback={<div className="py-16" />}>
      <SignInInner />
    </Suspense>
  )
}
