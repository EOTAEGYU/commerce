'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import { useRouter } from 'next/navigation'

// ── 타입 정의 ─────────────────────────────────────────────────────────────────

interface Coupon {
  id: number
  name: string
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT'
  discountValue: number
  minimumOrderAmount?: number
  maximumDiscountAmount?: number
  categoryName?: string
  expiresAt?: string
  status: 'ACTIVE' | 'USED' | 'EXPIRED'
  usedAt?: string
}

interface CouponTemplate {
  id: number
  name: string
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT'
  discountValue: number
  minimumOrderAmount?: number
  description?: string
}

type ActiveTab = 'active' | 'used' | 'expired'

// ── 헬퍼 함수 ─────────────────────────────────────────────────────────────────

function formatDiscount(discountType: 'PERCENTAGE' | 'FIXED_AMOUNT', discountValue: number): string {
  if (discountType === 'PERCENTAGE') {
    return `-${discountValue}%`
  }
  return `-₩${discountValue.toLocaleString('ko-KR')}`
}

function daysUntilExpiry(expiresAt: string): number {
  const now = new Date()
  const expiry = new Date(expiresAt)
  const diff = expiry.getTime() - now.getTime()
  return Math.floor(diff / (1000 * 60 * 60 * 24))
}

function formatExpiry(expiresAt?: string): string {
  if (!expiresAt) return ''
  const days = daysUntilExpiry(expiresAt)
  if (days < 0) return '만료'
  return `D-${days}`
}

function buildConditionText(coupon: Coupon | CouponTemplate, expiresAt?: string): string {
  const parts: string[] = []
  if (coupon.minimumOrderAmount) {
    parts.push(`₩${coupon.minimumOrderAmount.toLocaleString('ko-KR')} 이상`)
  }
  if ('maximumDiscountAmount' in coupon && coupon.maximumDiscountAmount) {
    parts.push(`최대 ₩${coupon.maximumDiscountAmount.toLocaleString('ko-KR')}`)
  }
  if (expiresAt) {
    parts.push(`~${expiresAt.slice(0, 10)}`)
  }
  return parts.join(' · ')
}

// ── 쿠폰 카드 (보유) ─────────────────────────────────────────────────────────

function CouponCard({ coupon, dimmed }: { coupon: Coupon; dimmed?: boolean }) {
  const dDayText = coupon.expiresAt ? formatExpiry(coupon.expiresAt) : ''
  const days = coupon.expiresAt ? daysUntilExpiry(coupon.expiresAt) : null
  const dDayClass = days !== null && days <= 7 ? 'text-red-500' : 'text-zinc-400'
  const conditionText = buildConditionText(coupon, coupon.expiresAt)

  return (
    <div className={`border border-dashed border-zinc-200 rounded p-4 flex gap-3 items-start ${dimmed ? 'opacity-60' : ''}`}>
      <div className="flex-1 min-w-0">
        <p className="text-2xl font-bold text-zinc-900">
          {formatDiscount(coupon.discountType, coupon.discountValue)}
        </p>
        <p className="text-sm mt-1 text-zinc-800 truncate">{coupon.name}</p>
        {conditionText && (
          <p className="text-xs text-zinc-400 mt-1">{conditionText}</p>
        )}
      </div>
      {dDayText && (
        <span className={`text-xs font-mono shrink-0 ${dDayClass}`}>{dDayText}</span>
      )}
    </div>
  )
}

// ── 발급 가능 쿠폰 카드 ──────────────────────────────────────────────────────

function IssuableCouponCard({
  template,
  isIssued,
  isPending,
  onIssue,
}: {
  template: CouponTemplate
  isIssued: boolean
  isPending: boolean
  onIssue: (id: number) => void
}) {
  const conditionText = buildConditionText(template)

  return (
    <div className="border border-zinc-900 rounded p-4 flex gap-3 items-center">
      <div className="flex-1 min-w-0">
        <p className="text-2xl font-bold text-zinc-900">
          {formatDiscount(template.discountType, template.discountValue)}
        </p>
        <p className="text-sm mt-1 text-zinc-800 truncate">{template.name}</p>
        {conditionText && (
          <p className="text-xs text-zinc-400 mt-1">{conditionText}</p>
        )}
      </div>
      {isIssued ? (
        <span className="text-xs text-zinc-400 shrink-0">받기 완료</span>
      ) : (
        <button
          type="button"
          disabled={isPending}
          onClick={() => onIssue(template.id)}
          className="shrink-0 bg-zinc-900 text-white text-xs px-4 py-2 rounded hover:bg-zinc-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          받기
        </button>
      )}
    </div>
  )
}

// ── 탭 버튼 ──────────────────────────────────────────────────────────────────

function TabButton({
  label,
  count,
  active,
  onClick,
}: {
  label: string
  count: number
  active: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`pb-2.5 px-1 text-sm font-medium flex items-center gap-1.5 border-b-2 transition-colors ${
        active
          ? 'border-zinc-900 text-zinc-900'
          : 'border-transparent text-zinc-400 hover:text-zinc-600'
      }`}
    >
      {label}
      <span
        className={`text-xs px-1.5 py-0.5 rounded-full font-mono ${
          active ? 'bg-zinc-900 text-white' : 'bg-zinc-100 text-zinc-500'
        }`}
      >
        {count}
      </span>
    </button>
  )
}

// ── 메인 페이지 ───────────────────────────────────────────────────────────────

export default function CouponsPage() {
  const { user } = useAuthStore()
  const router = useRouter()
  const queryClient = useQueryClient()

  const [activeTab, setActiveTab] = useState<ActiveTab>('active')
  const [issuedIds, setIssuedIds] = useState<Set<number>>(new Set())

  // 보유 쿠폰 조회
  const { data: couponsData, isLoading: couponsLoading } = useQuery({
    queryKey: ['my-coupons'],
    queryFn: () => apiFetch<Coupon[]>('/api/coupons/me'),
    enabled: !!user,
    staleTime: 60_000,
    retry: 1,
  })

  // 발급 가능 쿠폰 조회 (API가 없을 경우 빈 배열로 graceful degradation)
  const { data: issuableData } = useQuery({
    queryKey: ['issuable-coupons'],
    queryFn: () => apiFetch<CouponTemplate[]>('/api/coupons/issuable'),
    enabled: !!user,
    staleTime: 60_000,
    retry: 0,
  })

  // 쿠폰 발급 mutation
  const issueMutation = useMutation({
    mutationFn: (templateId: number) =>
      apiFetch<void>(`/api/coupons/${templateId}/issue`, { method: 'POST' }),
    onSuccess: (_, templateId) => {
      setIssuedIds((prev) => new Set(prev).add(templateId))
      queryClient.invalidateQueries({ queryKey: ['my-coupons'] })
      queryClient.invalidateQueries({ queryKey: ['issuable-coupons'] })
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        if (error.code === 'UNAUTHORIZED') {
          router.replace('/signin')
          return
        }
        alert(error.message)
      }
    },
  })

  const coupons = couponsData ?? []
  const issuableCoupons = issuableData ?? []

  const activeCoupons = coupons.filter((c) => c.status === 'ACTIVE')
  const usedCoupons = coupons.filter((c) => c.status === 'USED')
  const expiredCoupons = coupons.filter((c) => c.status === 'EXPIRED')

  const tabCoupons: Record<ActiveTab, Coupon[]> = {
    active: activeCoupons,
    used: usedCoupons,
    expired: expiredCoupons,
  }

  const currentCoupons = tabCoupons[activeTab]

  if (couponsLoading) {
    return <div className="text-center py-8 text-zinc-500">로딩 중...</div>
  }

  return (
    <div>
      <h1 className="text-xl font-bold mb-4">쿠폰함</h1>

      {/* 탭 스위처 */}
      <div className="flex gap-6 border-b border-zinc-200 mb-6">
        <TabButton
          label="보유"
          count={activeCoupons.length}
          active={activeTab === 'active'}
          onClick={() => setActiveTab('active')}
        />
        <TabButton
          label="사용완료"
          count={usedCoupons.length}
          active={activeTab === 'used'}
          onClick={() => setActiveTab('used')}
        />
        <TabButton
          label="만료"
          count={expiredCoupons.length}
          active={activeTab === 'expired'}
          onClick={() => setActiveTab('expired')}
        />
      </div>

      {/* 탭 콘텐츠 */}
      {currentCoupons.length === 0 ? (
        <p className="text-sm text-zinc-400 text-center py-16">
          {activeTab === 'active' && '보유 쿠폰이 없습니다'}
          {activeTab === 'used' && '사용완료 쿠폰이 없습니다'}
          {activeTab === 'expired' && '만료된 쿠폰이 없습니다'}
        </p>
      ) : (
        <div className="grid grid-cols-2 gap-3">
          {currentCoupons.map((coupon) => (
            <CouponCard
              key={coupon.id}
              coupon={coupon}
              dimmed={activeTab !== 'active'}
            />
          ))}
        </div>
      )}

      {/* 발급 가능 쿠폰 섹션 (보유 탭 + 발급 가능 쿠폰이 있을 때만) */}
      {activeTab === 'active' && issuableCoupons.length > 0 && (
        <section className="mt-8">
          <h3 className="text-base font-bold mb-4">
            발급 가능한 쿠폰
            <span className="ml-2 text-sm font-normal text-zinc-400">
              ({issuableCoupons.length})
            </span>
          </h3>
          <div className="grid grid-cols-2 gap-3">
            {issuableCoupons.map((template) => (
              <IssuableCouponCard
                key={template.id}
                template={template}
                isIssued={issuedIds.has(template.id)}
                isPending={issueMutation.isPending}
                onIssue={(id) => issueMutation.mutate(id)}
              />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
