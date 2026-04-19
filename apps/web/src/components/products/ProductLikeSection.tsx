'use client'

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'
import LikeButton from './LikeButton'

type Props = {
  productId: number
}

export default function ProductLikeSection({ productId }: Props) {
  const user = useAuthStore((s) => s.user)

  const { data } = useQuery({
    queryKey: ['likes', 'status', [productId]],
    queryFn: () =>
      apiFetch<Record<string, boolean>>(`/api/likes/status?productIds=${productId}`),
    enabled: !!user,
  })

  const isLiked = data?.[String(productId)] ?? false

  return (
    <LikeButton
      productId={productId}
      initialLiked={isLiked}
      showCount
    />
  )
}
