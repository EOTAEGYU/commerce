'use client'

import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import PaginationBar from '@/components/products/PaginationBar'

// ── 타입 정의 ─────────────────────────────────────────────────────────────────

interface PointBalance {
  balance: number
  pendingAmount?: number
}

interface PointHistory {
  id: number
  type: string
  amount: number
  balance: number
  description?: string
  createdAt: string
}

interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
}

type ActiveTab = 'all' | 'earn' | 'use' | 'expire'

// ── 포인트 타입 한국어 매핑 ──────────────────────────────────────────────────

const POINT_TYPE_LABELS: Record<string, string> = {
  EARN_ORDER: '주문 적립',
  EARN_REVIEW: '리뷰 작성 적립',
  EARN_REVIEW_PHOTO: '리뷰 사진 적립',
  EARN_SIGNUP: '회원가입 적립',
  EARN_EVENT: '이벤트 적립',
  USE_PAYMENT: '결제 사용',
  USE_MANUAL: '수동 차감',
  CANCEL_REFUND: '취소 환급',
  EXPIRE: '만료',
}

// 적립 타입인지 판별 (금액 색상 결정)
function isEarnType(type: string): boolean {
  return type.startsWith('EARN_') || type === 'CANCEL_REFUND'
}

// ── 탭 버튼 ──────────────────────────────────────────────────────────────────

function TabButton({
  label,
  active,
  onClick,
}: {
  label: string
  active: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`pb-2.5 px-1 text-sm font-medium border-b-2 transition-colors ${
        active
          ? 'border-zinc-900 text-zinc-900'
          : 'border-transparent text-zinc-400 hover:text-zinc-600'
      }`}
    >
      {label}
    </button>
  )
}

// ── 메인 페이지 ───────────────────────────────────────────────────────────────

export default function PointsPage() {
  const { user } = useAuthStore()
  const [activeTab, setActiveTab] = useState<ActiveTab>('all')
  const [page, setPage] = useState(0)

  // 포인트 잔액 조회
  const { data: balanceData, isLoading: balanceLoading } = useQuery({
    queryKey: ['my-points'],
    queryFn: () => apiFetch<PointBalance>('/api/points/me'),
    enabled: !!user,
    staleTime: 60_000,
    retry: 1,
  })

  // 포인트 이력 조회
  const historyParams = new URLSearchParams()
  historyParams.set('page', String(page))
  historyParams.set('size', '20')
  if (activeTab !== 'all') {
    historyParams.set('type', activeTab.toUpperCase())
  }

  const { data: historyData, isLoading: historyLoading } = useQuery({
    queryKey: ['my-point-history', activeTab, page],
    queryFn: () =>
      apiFetch<PageResponse<PointHistory>>(`/api/points/history?${historyParams.toString()}`),
    enabled: !!user,
    staleTime: 60_000,
    retry: 1,
  })

  function handleTabChange(tab: ActiveTab) {
    setActiveTab(tab)
    setPage(0)
  }

  const balance = balanceData?.balance ?? 0
  const pendingAmount = balanceData?.pendingAmount ?? 0
  const histories = historyData?.content ?? []
  const totalPages = historyData?.totalPages ?? 0

  return (
    <div>
      <h1 className="text-xl font-bold mb-4">포인트</h1>

      {/* 잔액 카드 */}
      <div className="border border-zinc-900 rounded-lg p-6 mb-6 flex items-center gap-6">
        <div className="flex-1 min-w-0">
          <p className="text-[10px] font-mono tracking-widest text-zinc-400 uppercase">
            Current Balance
          </p>
          {balanceLoading ? (
            <p className="text-5xl font-bold text-zinc-900 mt-1">-</p>
          ) : (
            <p className="text-5xl font-bold text-zinc-900 mt-1">
              {balance.toLocaleString('ko-KR')}P
            </p>
          )}
        </div>
        {pendingAmount > 0 && (
          <div className="text-right shrink-0">
            <p className="text-xs text-zinc-400">적립 예정</p>
            <p className="text-sm font-semibold text-zinc-600 mt-0.5">
              +{pendingAmount.toLocaleString('ko-KR')}P
            </p>
          </div>
        )}
      </div>

      {/* 탭 스위처 */}
      <div className="flex gap-6 border-b border-zinc-200 mb-4">
        <TabButton label="전체" active={activeTab === 'all'} onClick={() => handleTabChange('all')} />
        <TabButton label="적립" active={activeTab === 'earn'} onClick={() => handleTabChange('earn')} />
        <TabButton label="사용" active={activeTab === 'use'} onClick={() => handleTabChange('use')} />
        <TabButton label="만료" active={activeTab === 'expire'} onClick={() => handleTabChange('expire')} />
      </div>

      {/* 이력 테이블 */}
      {historyLoading ? (
        <div className="text-center py-8 text-zinc-500">로딩 중...</div>
      ) : histories.length === 0 ? (
        <div className="text-sm text-zinc-400 text-center py-10">포인트 내역이 없습니다</div>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr>
              <th className="text-[10px] font-mono tracking-wider text-zinc-400 uppercase text-left py-2 px-3 border-b border-zinc-200">
                Date
              </th>
              <th className="text-[10px] font-mono tracking-wider text-zinc-400 uppercase text-left py-2 px-3 border-b border-zinc-200">
                Type
              </th>
              <th className="text-[10px] font-mono tracking-wider text-zinc-400 uppercase text-left py-2 px-3 border-b border-zinc-200">
                Note
              </th>
              <th className="text-[10px] font-mono tracking-wider text-zinc-400 uppercase text-right py-2 px-3 border-b border-zinc-200">
                Amount
              </th>
              <th className="text-[10px] font-mono tracking-wider text-zinc-400 uppercase text-right py-2 px-3 border-b border-zinc-200">
                Balance
              </th>
            </tr>
          </thead>
          <tbody>
            {histories.map((item) => (
              <tr key={item.id} className="border-b border-zinc-100 last:border-0">
                <td className="text-xs text-zinc-500 py-3 px-3">
                  {new Date(item.createdAt).toLocaleDateString('ko-KR')}
                </td>
                <td className="text-xs font-mono text-zinc-500 py-3 px-3">
                  {POINT_TYPE_LABELS[item.type] ?? item.type}
                </td>
                <td className="text-xs text-zinc-600 py-3 px-3 max-w-[160px] truncate">
                  {item.description ?? '-'}
                </td>
                <td className={`text-sm font-bold text-right py-3 px-3 ${isEarnType(item.type) ? 'text-green-700' : 'text-red-600'}`}>
                  {isEarnType(item.type) ? '+' : '-'}{item.amount.toLocaleString('ko-KR')}P
                </td>
                <td className="text-xs text-zinc-500 text-right py-3 px-3">
                  {item.balance.toLocaleString('ko-KR')}P
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* 페이지네이션 */}
      <PaginationBar
        totalPages={totalPages}
        currentPage={page}
        basePath="/my/points"
      />
    </div>
  )
}
