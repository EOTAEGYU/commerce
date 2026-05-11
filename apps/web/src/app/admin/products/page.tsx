'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import type { components } from '@/types/api'

type ProductResponse = components['schemas']['ProductResponse']
type ProductOptionRequest = components['schemas']['ProductOptionRequest']
type CategoryResponse = components['schemas']['CategoryResponse']
type PageProductResponse = components['schemas']['PageProductResponse']

// ─── 모달 공통 오버레이 ──────────────────────────────────────────────────────────
function Modal({ title, onClose, children }: {
  title: string
  onClose: () => void
  children: React.ReactNode
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-2xl rounded-xl bg-white shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2 className="text-lg font-semibold text-zinc-900">{title}</h2>
          <button onClick={onClose} className="text-zinc-400 hover:text-zinc-700">✕</button>
        </div>
        <div className="px-6 py-5">{children}</div>
      </div>
    </div>
  )
}

// ─── 옵션 행 ─────────────────────────────────────────────────────────────────
function OptionRow({
  option,
  index,
  onChange,
  onRemove,
}: {
  option: ProductOptionRequest
  index: number
  onChange: (i: number, field: keyof ProductOptionRequest, value: string) => void
  onRemove: (i: number) => void
}) {
  return (
    <div className="flex gap-2 items-center">
      <input
        className="flex-1 rounded border border-zinc-300 px-2 py-1.5 text-sm"
        placeholder="사이즈"
        value={option.size}
        onChange={e => onChange(index, 'size', e.target.value)}
      />
      <input
        className="flex-1 rounded border border-zinc-300 px-2 py-1.5 text-sm"
        placeholder="컬러"
        value={option.color}
        onChange={e => onChange(index, 'color', e.target.value)}
      />
      <input
        className="w-24 rounded border border-zinc-300 px-2 py-1.5 text-sm"
        placeholder="재고"
        type="number"
        min={0}
        value={option.stock ?? 0}
        onChange={e => onChange(index, 'stock', e.target.value)}
      />
      <button
        type="button"
        onClick={() => onRemove(index)}
        className="text-zinc-400 hover:text-red-500"
      >
        ✕
      </button>
    </div>
  )
}

// ─── 상품 등록/수정 폼 ───────────────────────────────────────────────────────────
type ProductFormData = {
  name: string
  description: string
  price: string
  categoryId: string
  imageUrl: string
  options: ProductOptionRequest[]
}

function emptyForm(): ProductFormData {
  return { name: '', description: '', price: '', categoryId: '', imageUrl: '', options: [] }
}

function productToForm(p: ProductResponse): ProductFormData {
  return {
    name: p.name ?? '',
    description: p.description ?? '',
    price: String(p.price ?? ''),
    categoryId: String(p.categoryId ?? ''),
    imageUrl: p.imageUrl ?? '',
    options: (p.options ?? []).map((o) => ({ size: o.size ?? '', color: o.color ?? '', stock: o.stock ?? 0 })),
  }
}

function ProductForm({
  initialData,
  categories,
  onSubmit,
  isSubmitting,
}: {
  initialData: ProductFormData
  categories: CategoryResponse[]
  onSubmit: (data: ProductFormData) => void
  isSubmitting: boolean
}) {
  const [form, setForm] = useState<ProductFormData>(initialData)

  function setField<K extends keyof ProductFormData>(key: K, value: ProductFormData[K]) {
    setForm(prev => ({ ...prev, [key]: value }))
  }

  function handleOptionChange(i: number, field: keyof ProductOptionRequest, value: string) {
    const next = form.options.map((o, idx) =>
      idx === i ? { ...o, [field]: field === 'stock' ? Number(value) : value } : o
    )
    setField('options', next)
  }

  function addOption() {
    setField('options', [...form.options, { size: '', color: '', stock: 0 }])
  }

  function removeOption(i: number) {
    setField('options', form.options.filter((_, idx) => idx !== i))
  }

  // 대분류만 추출 (children이 있는 카테고리 = 대분류, children이 없고 parentId가 있으면 소분류)
  // CategoryResponse의 children 구조를 활용해 소분류 목록 추출
  const flatOptions: { id: number; label: string }[] = []
  categories.forEach(cat => {
    flatOptions.push({ id: cat.id!, label: cat.name! })
    const children = cat.children as CategoryResponse[] | undefined
    if (children && children.length > 0) {
      children.forEach(child => {
        const c = child as CategoryResponse
        flatOptions.push({ id: c.id!, label: `  └ ${c.name}` })
      })
    }
  })

  return (
    <form
      onSubmit={e => { e.preventDefault(); onSubmit(form) }}
      className="space-y-4"
    >
      <div>
        <label className="block text-sm font-medium text-zinc-700 mb-1">상품명 *</label>
        <input
          required
          className="w-full rounded border border-zinc-300 px-3 py-2 text-sm"
          value={form.name}
          onChange={e => setField('name', e.target.value)}
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-zinc-700 mb-1">설명</label>
        <textarea
          rows={3}
          className="w-full rounded border border-zinc-300 px-3 py-2 text-sm resize-none"
          value={form.description}
          onChange={e => setField('description', e.target.value)}
        />
      </div>

      <div className="flex gap-4">
        <div className="flex-1">
          <label className="block text-sm font-medium text-zinc-700 mb-1">가격 (원) *</label>
          <input
            required
            type="number"
            min={0}
            className="w-full rounded border border-zinc-300 px-3 py-2 text-sm"
            value={form.price}
            onChange={e => setField('price', e.target.value)}
          />
        </div>

        <div className="flex-1">
          <label className="block text-sm font-medium text-zinc-700 mb-1">카테고리 *</label>
          <select
            required
            className="w-full rounded border border-zinc-300 px-3 py-2 text-sm"
            value={form.categoryId}
            onChange={e => setField('categoryId', e.target.value)}
          >
            <option value="">선택</option>
            {flatOptions.map(opt => (
              <option key={opt.id} value={opt.id}>{opt.label}</option>
            ))}
          </select>
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-zinc-700 mb-1">이미지 URL</label>
        <input
          className="w-full rounded border border-zinc-300 px-3 py-2 text-sm"
          value={form.imageUrl}
          onChange={e => setField('imageUrl', e.target.value)}
        />
      </div>

      <div>
        <div className="flex items-center justify-between mb-2">
          <label className="text-sm font-medium text-zinc-700">옵션 (사이즈 / 컬러 / 재고)</label>
          <button
            type="button"
            onClick={addOption}
            className="text-xs font-medium text-zinc-600 hover:text-zinc-900 underline"
          >
            + 옵션 추가
          </button>
        </div>
        <div className="space-y-2">
          {form.options.length === 0 && (
            <p className="text-xs text-zinc-400">옵션을 추가하세요.</p>
          )}
          {form.options.map((opt, i) => (
            <OptionRow
              key={i}
              option={opt}
              index={i}
              onChange={handleOptionChange}
              onRemove={removeOption}
            />
          ))}
        </div>
      </div>

      <div className="flex justify-end pt-2">
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded bg-zinc-900 px-5 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
        >
          {isSubmitting ? '저장 중...' : '저장'}
        </button>
      </div>
    </form>
  )
}

// ─── 메인 페이지 ─────────────────────────────────────────────────────────────
export default function AdminProductsPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [modal, setModal] = useState<null | 'create' | 'edit'>(null)
  const [editTarget, setEditTarget] = useState<ProductResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ProductResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  // 상품 목록
  const { data, isLoading } = useQuery({
    queryKey: ['admin-products', page],
    queryFn: () =>
      apiFetch<PageProductResponse>(`/api/products?page=${page}&size=10`),
  })

  // 카테고리 목록 (폼 select용)
  const { data: categories = [] } = useQuery({
    queryKey: ['categories'],
    queryFn: () => apiFetch<CategoryResponse[]>('/api/categories'),
  })

  // 등록
  const createMutation = useMutation({
    mutationFn: (body: object) => apiFetch<ProductResponse>('/api/products', { method: 'POST', body: JSON.stringify(body) }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-products'] }); setModal(null); setError(null) },
    onError: (e: Error) => setError(e.message),
  })

  // 수정
  const updateMutation = useMutation({
    mutationFn: ({ id, body }: { id: number; body: object }) =>
      apiFetch<ProductResponse>(`/api/products/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-products'] }); setModal(null); setError(null) },
    onError: (e: Error) => setError(e.message),
  })

  // 삭제
  const deleteMutation = useMutation({
    mutationFn: (id: number) => apiFetch<void>(`/api/products/${id}`, { method: 'DELETE' }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-products'] }); setDeleteTarget(null) },
    onError: (e: Error) => setError(e.message),
  })

  function handleCreate(form: ProductFormData) {
    if (!form.options.length) { setError('옵션을 1개 이상 추가해주세요.'); return }
    createMutation.mutate({
      name: form.name,
      description: form.description || undefined,
      price: Number(form.price),
      categoryId: Number(form.categoryId),
      imageUrl: form.imageUrl || undefined,
      options: form.options,
    })
  }

  function handleUpdate(form: ProductFormData) {
    if (!editTarget) return
    if (!form.options.length) { setError('옵션을 1개 이상 추가해주세요.'); return }
    updateMutation.mutate({
      id: editTarget.id!,
      body: {
        name: form.name,
        description: form.description || undefined,
        price: Number(form.price),
        categoryId: Number(form.categoryId),
        imageUrl: form.imageUrl || undefined,
        options: form.options,
      },
    })
  }

  const products: ProductResponse[] = data?.content ?? []
  const totalPages = data?.totalPages ?? 1

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-zinc-900">상품 관리</h1>
        <button
          onClick={() => { setError(null); setModal('create') }}
          className="rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700"
        >
          + 상품 등록
        </button>
      </div>

      {error && (
        <p className="mb-4 rounded bg-red-50 px-4 py-2 text-sm text-red-600">{error}</p>
      )}

      {/* 상품 테이블 */}
      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-12 animate-pulse rounded bg-zinc-100" />
          ))}
        </div>
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-zinc-200">
            <table className="w-full text-sm">
              <thead className="bg-zinc-50 text-left text-xs font-semibold uppercase tracking-wider text-zinc-500">
                <tr>
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">상품명</th>
                  <th className="px-4 py-3">가격</th>
                  <th className="px-4 py-3">카테고리</th>
                  <th className="px-4 py-3">옵션 수</th>
                  <th className="px-4 py-3 text-right">작업</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100">
                {products.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-zinc-400">
                      등록된 상품이 없습니다.
                    </td>
                  </tr>
                )}
                {products.map(p => (
                  <tr key={p.id} className="hover:bg-zinc-50">
                    <td className="px-4 py-3 text-zinc-400">{p.id}</td>
                    <td className="px-4 py-3 font-medium text-zinc-900">{p.name}</td>
                    <td className="px-4 py-3 text-zinc-700">{(p.price ?? 0).toLocaleString('ko-KR')}원</td>
                    <td className="px-4 py-3 text-zinc-500">{p.categoryId}</td>
                    <td className="px-4 py-3 text-zinc-500">{(p.options ?? []).length}개</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => { setEditTarget(p); setError(null); setModal('edit') }}
                        className="mr-3 text-xs font-medium text-zinc-600 hover:text-zinc-900 underline"
                      >
                        수정
                      </button>
                      <button
                        onClick={() => setDeleteTarget(p)}
                        className="text-xs font-medium text-red-500 hover:text-red-700 underline"
                      >
                        삭제
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="mt-4 flex justify-center gap-2">
              {Array.from({ length: totalPages }).map((_, i) => (
                <button
                  key={i}
                  onClick={() => setPage(i)}
                  className={`rounded px-3 py-1.5 text-sm font-medium transition-colors ${
                    page === i
                      ? 'bg-zinc-900 text-white'
                      : 'bg-zinc-100 text-zinc-700 hover:bg-zinc-200'
                  }`}
                >
                  {i + 1}
                </button>
              ))}
            </div>
          )}
        </>
      )}

      {/* 등록 모달 */}
      {modal === 'create' && (
        <Modal title="상품 등록" onClose={() => setModal(null)}>
          {error && <p className="mb-3 text-sm text-red-500">{error}</p>}
          <ProductForm
            initialData={emptyForm()}
            categories={categories as CategoryResponse[]}
            onSubmit={handleCreate}
            isSubmitting={createMutation.isPending}
          />
        </Modal>
      )}

      {/* 수정 모달 */}
      {modal === 'edit' && editTarget && (
        <Modal title="상품 수정" onClose={() => setModal(null)}>
          {error && <p className="mb-3 text-sm text-red-500">{error}</p>}
          <ProductForm
            initialData={productToForm(editTarget)}
            categories={categories as CategoryResponse[]}
            onSubmit={handleUpdate}
            isSubmitting={updateMutation.isPending}
          />
        </Modal>
      )}

      {/* 삭제 확인 모달 */}
      {deleteTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-sm rounded-xl bg-white p-6 shadow-xl">
            <h3 className="text-base font-semibold text-zinc-900">상품 삭제</h3>
            <p className="mt-2 text-sm text-zinc-600">
              <span className="font-medium">{deleteTarget.name}</span>을(를) 삭제하시겠습니까?
              이 작업은 되돌릴 수 없습니다.
            </p>
            {error && <p className="mt-2 text-sm text-red-500">{error}</p>}
            <div className="mt-5 flex justify-end gap-3">
              <button
                onClick={() => { setDeleteTarget(null); setError(null) }}
                className="rounded border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-50"
              >
                취소
              </button>
              <button
                onClick={() => deleteMutation.mutate(deleteTarget.id!)}
                disabled={deleteMutation.isPending}
                className="rounded bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
              >
                {deleteMutation.isPending ? '삭제 중...' : '삭제'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
