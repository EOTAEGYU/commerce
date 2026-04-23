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

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
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
    <div className="min-h-screen bg-zinc-50 flex items-center justify-center">
      <div className="w-full max-w-[400px] bg-white border border-zinc-200 rounded-lg p-8">
        {/* 로고 */}
        <p className="text-2xl font-bold text-zinc-900 text-center mb-5">commerce</p>

        {/* 탭 스위처 */}
        <div className="flex border-b border-zinc-200 mb-6">
          <Link
            href="/signin"
            className="flex-1 text-center text-sm border-b-2 border-zinc-900 font-bold text-zinc-900 pb-2"
          >
            로그인
          </Link>
          <Link
            href="/signup"
            className="flex-1 text-center text-sm text-zinc-400 pb-2"
          >
            회원가입
          </Link>
        </div>

        {/* 폼 */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* 아이디 */}
          <div>
            <label
              htmlFor="username"
              className="block text-[10px] uppercase tracking-widest text-zinc-400 font-mono mb-1"
            >
              아이디
            </label>
            <input
              id="username"
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="아이디를 입력하세요"
              className="w-full border border-zinc-200 rounded px-3.5 py-3 text-sm text-zinc-800 outline-none focus:border-zinc-500"
            />
          </div>

          {/* 비밀번호 */}
          <div>
            <label
              htmlFor="password"
              className="block text-[10px] uppercase tracking-widest text-zinc-400 font-mono mb-1"
            >
              Password
            </label>
            <input
              id="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호를 입력하세요"
              className="w-full border border-zinc-200 rounded px-3.5 py-3 text-sm text-zinc-800 outline-none focus:border-zinc-500"
            />
          </div>

          {/* 로그인 유지 + 비밀번호 찾기 */}
          <div className="flex items-center justify-between">
            <label className="flex items-center gap-1.5 cursor-pointer">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                className="accent-zinc-900"
              />
              <span className="text-xs text-zinc-400">로그인 유지</span>
            </label>
            <a href="#" className="text-xs text-zinc-400 hover:text-zinc-600">
              비밀번호 찾기
            </a>
          </div>

          {/* 에러 메시지 */}
          {error && (
            <p className="rounded bg-red-50 px-3 py-2 text-xs text-red-500">{error}</p>
          )}

          {/* 로그인 버튼 */}
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-zinc-900 text-white py-3 text-sm font-medium rounded hover:bg-zinc-700 disabled:opacity-50 transition-colors"
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        {/* OR 구분선 */}
        <div className="flex items-center gap-3 my-5">
          <span className="flex-1 border-t border-zinc-200" />
          <span className="text-[10px] font-mono text-zinc-400 uppercase tracking-widest whitespace-nowrap">
            Or continue with
          </span>
          <span className="flex-1 border-t border-zinc-200" />
        </div>

        {/* 소셜 버튼 */}
        <div className="flex gap-2">
          <button
            type="button"
            className="flex-1 py-2.5 border border-zinc-200 text-sm rounded hover:bg-zinc-50 transition-colors"
          >
            카카오
          </button>
          <button
            type="button"
            className="flex-1 py-2.5 border border-zinc-200 text-sm rounded hover:bg-zinc-50 transition-colors"
          >
            네이버
          </button>
          <button
            type="button"
            className="flex-1 py-2.5 border border-zinc-200 text-sm rounded hover:bg-zinc-50 transition-colors"
          >
            구글
          </button>
        </div>
      </div>
    </div>
  )
}

export default function SignInPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-zinc-50" />}>
      <SignInInner />
    </Suspense>
  )
}
