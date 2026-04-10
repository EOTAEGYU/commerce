import { useAuthStore } from '@/store/auth'
import type { ApiResponse } from '@/types/api'

const BASE_URL = process.env.NEXT_PUBLIC_API_URL

if (!BASE_URL) {
  throw new Error('NEXT_PUBLIC_API_URL 환경변수가 설정되지 않았습니다.')
}

// API 에러 클래스 — code 필드를 보존해 UI에서 에러 종류별 분기 처리 가능
// 예: error instanceof ApiError && error.code === 'OUT_OF_STOCK'
export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export async function apiFetch<T>(
  path: string,
  options?: RequestInit
): Promise<T> {
  // Zustand store에서 토큰 읽기 (React 훅 밖에서도 사용 가능한 getState())
  const token = useAuthStore.getState().token

  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })

  // 401: 토큰 만료 또는 미인증 → 스토어 초기화 후 에러 throw
  if (res.status === 401) {
    useAuthStore.getState().clearAuth()
    throw new ApiError('UNAUTHORIZED', '로그인이 필요합니다.')
  }

  const body: ApiResponse<T> = await res.json()

  if (!body.success || body.data === null) {
    throw new ApiError(
      body.error?.code ?? 'UNKNOWN_ERROR',
      body.error?.message ?? '알 수 없는 오류가 발생했습니다.'
    )
  }

  return body.data
}
