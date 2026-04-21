'use client'

import { Suspense, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'

function SignUpInner() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const setAuth = useAuthStore((s) => s.setAuth)

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      // 1) 회원가입
      await apiFetch('/api/users/signup', {
        method: 'POST',
        body: JSON.stringify({ name, email, password }),
      })

      // 2) 자동 로그인 — 회원가입 직후 signin API 재호출
      const { accessToken } = await apiFetch<{ accessToken: string; tokenType: string }>(
        '/api/users/signin',
        { method: 'POST', body: JSON.stringify({ email, password }) }
      )

      const user = await apiFetch<{ id: number; email: string; name: string; role: 'USER' | 'ADMIN' }>(
        '/api/users/me',
        { headers: { Authorization: `Bearer ${accessToken}` } }
      )

      setAuth(accessToken, user)

      const redirectUrl = searchParams.get('redirect') ?? '/'
      router.push(redirectUrl)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(
          err.code === 'DUPLICATE_EMAIL'
            ? '이미 사용 중인 이메일입니다.'
            : err.message
        )
      } else {
        setError('회원가입 중 오류가 발생했습니다.')
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
            className="flex-1 text-center text-sm text-zinc-400 pb-2"
          >
            로그인
          </Link>
          <Link
            href="/signup"
            className="flex-1 text-center text-sm border-b-2 border-zinc-900 font-bold text-zinc-900 pb-2"
          >
            회원가입
          </Link>
        </div>

        {/* 폼 */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* 이름 */}
          <div>
            <label
              htmlFor="name"
              className="block text-[10px] uppercase tracking-widest text-zinc-400 font-mono mb-1"
            >
              Name
            </label>
            <input
              id="name"
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="홍길동"
              className="w-full border border-zinc-200 rounded px-3.5 py-3 text-sm text-zinc-800 outline-none focus:border-zinc-500"
            />
          </div>

          {/* 이메일 */}
          <div>
            <label
              htmlFor="email"
              className="block text-[10px] uppercase tracking-widest text-zinc-400 font-mono mb-1"
            >
              Email
            </label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="example@email.com"
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
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="8자 이상, 숫자/영문 포함"
              className="w-full border border-zinc-200 rounded px-3.5 py-3 text-sm text-zinc-800 outline-none focus:border-zinc-500"
            />
          </div>

          {/* 약관 체크박스 */}
          <div className="flex flex-col gap-2">
            <label className="text-xs text-zinc-500 flex items-center gap-2 cursor-pointer">
              <input type="checkbox" required defaultChecked className="accent-zinc-900" />
              만 14세 이상입니다 (필수)
            </label>
            <label className="text-xs text-zinc-500 flex items-center gap-2 cursor-pointer">
              <input type="checkbox" required defaultChecked className="accent-zinc-900" />
              이용약관 동의 (필수)
            </label>
            <label className="text-xs text-zinc-500 flex items-center gap-2 cursor-pointer">
              <input type="checkbox" className="accent-zinc-900" />
              마케팅 수신 동의 (선택)
            </label>
          </div>

          {/* 에러 메시지 */}
          {error && (
            <p className="rounded bg-red-50 px-3 py-2 text-xs text-red-500">{error}</p>
          )}

          {/* 가입하기 버튼 */}
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-zinc-900 text-white py-3 text-sm font-medium rounded hover:bg-zinc-700 disabled:opacity-50 transition-colors"
          >
            {loading ? '가입 중...' : '가입하기'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default function SignUpPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-zinc-50" />}>
      <SignUpInner />
    </Suspense>
  )
}
