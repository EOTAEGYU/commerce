// ─── 백엔드 ApiResponse<T> 래퍼 ───────────────────────────────────────────────
// 백엔드 ApiResponse.kt: { success, data, error: ErrorResponse? }
// error는 { code: string, message: string } 구조

export type ApiErrorResponse = {
  code: string
  message: string
}

export type ApiResponse<T> = {
  success: boolean
  data: T | null
  error: ApiErrorResponse | null
}

// ─── 백엔드 Page<T> 래퍼 ─────────────────────────────────────────────────────
// Spring Data의 Page<T> 구조 (openapi-typescript로 자동 생성되지 않으므로 수동 정의)
export type PageResponse<T> = {
  content: T[]
  totalPages: number
  totalElements: number
  number: number // 현재 페이지 (0-indexed)
  size: number
  first: boolean
  last: boolean
}

// ─── 자동 생성 타입 재수출 ─────────────────────────────────────────────────────
// api.generated.ts는 `npm run generate:types`로 생성됨 (직접 수정 금지)
// 백엔드 미기동 상태에서는 이 파일이 없으므로 타입 생성 후 아래 주석 해제
export type { components } from './api.generated'
