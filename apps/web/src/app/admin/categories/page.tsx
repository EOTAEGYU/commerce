'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import type { components } from '@/types/api'

type CategoryResponse = components['schemas']['CategoryResponse'] & {
  children?: CategoryResponse[]
}

// ─── 카테고리 추가 폼 ─────────────────────────────────────────────────────────
function AddCategoryForm({
  parents,
  onSubmit,
  isSubmitting,
}: {
  parents: CategoryResponse[]
  onSubmit: (name: string, parentId: number | null, displayOrder: number) => void
  isSubmitting: boolean
}) {
  const [name, setName] = useState('')
  const [parentId, setParentId] = useState<string>('')
  const [displayOrder, setDisplayOrder] = useState<string>('0')

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) return
    onSubmit(name.trim(), parentId ? Number(parentId) : null, Number(displayOrder))
    setName('')
    setParentId('')
    setDisplayOrder('0')
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-wrap gap-3 items-end">
      <div>
        <label className="block text-xs font-medium text-zinc-600 mb-1">카테고리명 *</label>
        <input
          required
          className="rounded border border-zinc-300 px-3 py-2 text-sm w-44"
          placeholder="예: 상의, 반팔티셔츠"
          value={name}
          onChange={e => setName(e.target.value)}
        />
      </div>
      <div>
        <label className="block text-xs font-medium text-zinc-600 mb-1">상위 카테고리 (소분류인 경우)</label>
        <select
          className="rounded border border-zinc-300 px-3 py-2 text-sm w-40"
          value={parentId}
          onChange={e => setParentId(e.target.value)}
        >
          <option value="">없음 (대분류)</option>
          {parents.map(p => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="block text-xs font-medium text-zinc-600 mb-1">노출 순서</label>
        <input
          type="number"
          min={0}
          className="rounded border border-zinc-300 px-3 py-2 text-sm w-24"
          value={displayOrder}
          onChange={e => setDisplayOrder(e.target.value)}
        />
      </div>
      <button
        type="submit"
        disabled={isSubmitting}
        className="rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
      >
        {isSubmitting ? '추가 중...' : '추가'}
      </button>
    </form>
  )
}

// ─── 삭제 확인 모달 ───────────────────────────────────────────────────────────
function DeleteModal({
  category,
  onConfirm,
  onCancel,
  isDeleting,
  error,
}: {
  category: CategoryResponse
  onConfirm: () => void
  onCancel: () => void
  isDeleting: boolean
  error: string | null
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-sm rounded-xl bg-white p-6 shadow-xl">
        <h3 className="text-base font-semibold text-zinc-900">카테고리 삭제</h3>
        <p className="mt-2 text-sm text-zinc-600">
          <span className="font-medium">{category.name}</span>을(를) 삭제하시겠습니까?
        </p>
        {(category.children?.length ?? 0) > 0 && (
          <p className="mt-1 text-xs text-amber-600">
            ⚠ 소분류가 있는 카테고리는 삭제할 수 없습니다.
          </p>
        )}
        {error && <p className="mt-2 text-sm text-red-500">{error}</p>}
        <div className="mt-5 flex justify-end gap-3">
          <button
            onClick={onCancel}
            className="rounded border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-50"
          >
            취소
          </button>
          <button
            onClick={onConfirm}
            disabled={isDeleting}
            className="rounded bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
          >
            {isDeleting ? '삭제 중...' : '삭제'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ─── 메인 페이지 ─────────────────────────────────────────────────────────────
export default function AdminCategoriesPage() {
  const qc = useQueryClient()
  const [deleteTarget, setDeleteTarget] = useState<CategoryResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const { data: categories = [], isLoading } = useQuery({
    queryKey: ['categories'],
    queryFn: () => apiFetch<CategoryResponse[]>('/api/categories'),
  })

  const createMutation = useMutation({
    mutationFn: (body: object) =>
      apiFetch<CategoryResponse>('/api/categories', { method: 'POST', body: JSON.stringify(body) }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['categories'] }); setFormError(null) },
    onError: (e: Error) => setFormError(e.message),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => apiFetch<void>(`/api/categories/${id}`, { method: 'DELETE' }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['categories'] }); setDeleteTarget(null); setError(null) },
    onError: (e: Error) => setError(e.message),
  })

  function handleAdd(name: string, parentId: number | null, displayOrder: number) {
    createMutation.mutate({
      name,
      parentId: parentId ?? undefined,
      displayOrder,
    })
  }

  // 대분류만 추출 (추가 폼 select 옵션용)
  const topLevel = (categories as CategoryResponse[]).filter(c => !c.children || c.id !== undefined)
    // API는 전부 최상위로 반환하므로, children이 있는 것이 대분류
    // 실제론 모든 항목이 대분류로 오고 children 배열에 소분류가 들어 있음

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-zinc-900">카테고리 관리</h1>

      {/* 추가 폼 */}
      <div className="mb-8 rounded-lg border border-zinc-200 bg-zinc-50 p-5">
        <h2 className="mb-4 text-sm font-semibold text-zinc-700">카테고리 추가</h2>
        {formError && (
          <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-600">{formError}</p>
        )}
        <AddCategoryForm
          parents={categories as CategoryResponse[]}
          onSubmit={handleAdd}
          isSubmitting={createMutation.isPending}
        />
      </div>

      {/* 카테고리 트리 */}
      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-10 animate-pulse rounded bg-zinc-100" />
          ))}
        </div>
      ) : (categories as CategoryResponse[]).length === 0 ? (
        <p className="text-sm text-zinc-400">카테고리가 없습니다.</p>
      ) : (
        <div className="rounded-lg border border-zinc-200 overflow-hidden">
          {(categories as CategoryResponse[]).map(parent => (
            <div key={parent.id}>
              {/* 대분류 행 */}
              <div className="flex items-center justify-between border-b border-zinc-100 bg-white px-4 py-3">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-zinc-900">{parent.name}</span>
                  <span className="rounded-full bg-zinc-100 px-2 py-0.5 text-xs text-zinc-500">
                    대분류
                  </span>
                </div>
                <button
                  onClick={() => { setDeleteTarget(parent); setError(null) }}
                  className="text-xs text-red-400 hover:text-red-600 underline"
                >
                  삭제
                </button>
              </div>

              {/* 소분류 행들 */}
              {((parent.children ?? []) as CategoryResponse[]).map(child => (
                <div
                  key={child.id}
                  className="flex items-center justify-between border-b border-zinc-100 bg-zinc-50 px-4 py-2.5"
                >
                  <div className="flex items-center gap-2 pl-4">
                    <span className="text-zinc-400">└</span>
                    <span className="text-sm text-zinc-700">{child.name}</span>
                  </div>
                  <button
                    onClick={() => { setDeleteTarget(child); setError(null) }}
                    className="text-xs text-red-400 hover:text-red-600 underline"
                  >
                    삭제
                  </button>
                </div>
              ))}
            </div>
          ))}
        </div>
      )}

      {/* 삭제 확인 모달 */}
      {deleteTarget && (
        <DeleteModal
          category={deleteTarget}
          onConfirm={() => deleteMutation.mutate(deleteTarget.id!)}
          onCancel={() => { setDeleteTarget(null); setError(null) }}
          isDeleting={deleteMutation.isPending}
          error={error}
        />
      )}
    </div>
  )
}
