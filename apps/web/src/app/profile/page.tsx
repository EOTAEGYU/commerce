'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { apiFetch } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import type { components } from '@/types/api'

type UserResponse = components['schemas']['UserResponse']

export default function ProfilePage() {
  const router = useRouter()
  const { user, token, setAuth } = useAuthStore()
  const [name, setName] = useState(user?.name ?? '')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!user || !token) {
      router.replace('/signin')
    }
  }, [user, token, router])

  if (!user || !token) return null

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const trimmed = name.trim()
    if (!trimmed) {
      setError('이름을 입력해주세요.')
      return
    }
    setError('')
    setSuccess(false)
    setLoading(true)
    try {
      const updated = await apiFetch<UserResponse>('/api/users/me', {
        method: 'PUT',
        body: JSON.stringify({ name: trimmed }),
      })
      setAuth(token!, { ...user!, name: updated.name ?? trimmed })
      setName(updated.name ?? trimmed)
      setSuccess(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : '수정에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-16">
      <h1 className="mb-8 text-2xl font-bold text-zinc-900">프로필 수정</h1>

      <div className="mb-6 rounded-lg border border-zinc-200 bg-zinc-50 p-4 text-sm text-zinc-600">
        <p><span className="font-medium">이메일</span> {user.email}</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="name" className="mb-1 block text-sm font-medium text-zinc-700">
            이름
          </label>
          <input
            id="name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-500"
          />
        </div>

        {error && <p className="text-sm text-red-500">{error}</p>}
        {success && <p className="text-sm text-green-600">이름이 변경되었습니다.</p>}

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded bg-zinc-900 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
        >
          {loading ? '저장 중…' : '저장'}
        </button>
      </form>
    </div>
  )
}
