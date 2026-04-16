'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api/client'
import { useAuthStore } from '@/store/auth'

type LikeResponse = {
  productId: number
  liked: boolean
  likeCount: number
}

type Props = {
  productId: number
  initialLiked: boolean
  initialCount?: number
  showCount?: boolean
}

export default function LikeButton({ productId, initialLiked, initialCount = 0, showCount = false }: Props) {
  const router = useRouter()
  const queryClient = useQueryClient()
  const user = useAuthStore((s) => s.user)

  const [liked, setLiked] = useState(initialLiked)
  const [likeCount, setLikeCount] = useState(initialCount)

  const { mutate } = useMutation({
    mutationFn: () => apiFetch<LikeResponse>(`/api/likes/${productId}`, { method: 'POST' }),
    onMutate: () => {
      // 낙관적 업데이트
      const prevLiked = liked
      const prevCount = likeCount
      setLiked((v) => !v)
      setLikeCount((c) => (liked ? c - 1 : c + 1))
      return { prevLiked, prevCount }
    },
    onError: (_err, _vars, ctx) => {
      // 롤백
      if (ctx) {
        setLiked(ctx.prevLiked)
        setLikeCount(ctx.prevCount)
      }
    },
    onSuccess: (data) => {
      setLiked(data.liked)
      setLikeCount(data.likeCount)
      queryClient.invalidateQueries({ queryKey: ['likes', 'my'] })
    },
  })

  function handleClick(e: React.MouseEvent) {
    e.preventDefault()
    e.stopPropagation()
    if (!user) {
      router.push('/signin')
      return
    }
    mutate()
  }

  return (
    <button
      onClick={handleClick}
      className="flex items-center gap-0.5 rounded-full p-1 text-white/80 transition-colors hover:text-white drop-shadow"
      aria-label={liked ? '좋아요 취소' : '좋아요'}
    >
      <svg
        className="h-5 w-5"
        viewBox="0 0 24 24"
        stroke="currentColor"
        strokeWidth={1.5}
        fill={liked ? 'currentColor' : 'none'}
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z"
        />
      </svg>
      {showCount && likeCount > 0 && (
        <span className="text-xs leading-none">{likeCount}</span>
      )}
    </button>
  )
}
