'use client'

import { useState, useEffect, useCallback } from 'react'

const SLIDES = [
  {
    id: 1,
    title: 'Spring Drop',
    subtitle: 'Week 3',
    description: '봄을 담은 새로운 컬렉션',
    bgFrom: 'from-zinc-200',
    bgTo: 'to-zinc-100',
    pattern: 'bg-[repeating-linear-gradient(45deg,transparent,transparent_10px,rgba(0,0,0,0.03)_10px,rgba(0,0,0,0.03)_20px)]',
  },
  {
    id: 2,
    title: 'Summer Collection',
    subtitle: '2025',
    description: '여름의 시작, 지금 만나보세요',
    bgFrom: 'from-stone-200',
    bgTo: 'to-stone-100',
    pattern: 'bg-[repeating-linear-gradient(135deg,transparent,transparent_10px,rgba(0,0,0,0.03)_10px,rgba(0,0,0,0.03)_20px)]',
  },
  {
    id: 3,
    title: 'Best Sellers',
    subtitle: 'Pick',
    description: '지금 가장 인기있는 아이템',
    bgFrom: 'from-zinc-300',
    bgTo: 'to-zinc-100',
    pattern: 'bg-[repeating-linear-gradient(90deg,transparent,transparent_15px,rgba(0,0,0,0.03)_15px,rgba(0,0,0,0.03)_16px)]',
  },
]

export default function HeroBanner() {
  const [current, setCurrent] = useState(0)
  const [isPaused, setIsPaused] = useState(false)

  const next = useCallback(() => {
    setCurrent((c) => (c + 1) % SLIDES.length)
  }, [])

  const prev = useCallback(() => {
    setCurrent((c) => (c - 1 + SLIDES.length) % SLIDES.length)
  }, [])

  useEffect(() => {
    if (isPaused) return
    const timer = setInterval(next, 3000)
    return () => clearInterval(timer)
  }, [isPaused, next])

  const slide = SLIDES[current]

  return (
    <div
      className="relative mb-10 w-full overflow-hidden"
      style={{ aspectRatio: '16/7' }}
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
    >
      {/* 슬라이드 배경 */}
      {SLIDES.map((s, i) => (
        <div
          key={s.id}
          className={`absolute inset-0 bg-gradient-to-br ${s.bgFrom} ${s.bgTo} ${s.pattern} transition-opacity duration-700 ${
            i === current ? 'opacity-100' : 'opacity-0'
          }`}
        />
      ))}

      {/* 좌측 텍스트 */}
      <div className="absolute inset-0 flex items-center px-12">
        <div key={current} className="animate-[fadeInUp_0.5s_ease]">
          <p className="mb-1 text-xs font-mono uppercase tracking-[0.3em] text-zinc-400">
            {slide.subtitle}
          </p>
          <h2 className="text-5xl font-black tracking-tight text-zinc-900 leading-none">
            {slide.title}
          </h2>
          <p className="mt-3 text-sm text-zinc-500">{slide.description}</p>
          <button className="mt-6 inline-block rounded-none border border-zinc-900 bg-zinc-900 px-8 py-3 text-xs font-medium uppercase tracking-widest text-white transition-colors hover:bg-transparent hover:text-zinc-900">
            Shop Now
          </button>
        </div>
      </div>

      {/* 우측 하단 캡션 박스 */}
      <div className="absolute bottom-6 right-8 bg-white px-4 py-2.5 shadow-sm">
        <p className="text-[10px] font-mono uppercase tracking-widest text-zinc-400">New Collection</p>
        <p className="text-sm font-bold text-zinc-900">{slide.title} · {slide.subtitle}</p>
      </div>

      {/* 좌우 화살표 */}
      <button
        onClick={prev}
        className="absolute left-4 top-1/2 -translate-y-1/2 flex h-9 w-9 items-center justify-center rounded-full bg-white/80 text-zinc-700 shadow-sm transition-colors hover:bg-white"
        aria-label="이전 슬라이드"
      >
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <button
        onClick={next}
        className="absolute right-4 top-1/2 -translate-y-1/2 flex h-9 w-9 items-center justify-center rounded-full bg-white/80 text-zinc-700 shadow-sm transition-colors hover:bg-white"
        aria-label="다음 슬라이드"
      >
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
        </svg>
      </button>

      {/* 하단 점 인디케이터 */}
      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-1.5">
        {SLIDES.map((_, i) => (
          <button
            key={i}
            onClick={() => setCurrent(i)}
            className={`h-1.5 rounded-full transition-all duration-300 ${
              i === current ? 'w-6 bg-zinc-900' : 'w-1.5 bg-zinc-400'
            }`}
            aria-label={`슬라이드 ${i + 1}`}
          />
        ))}
      </div>
    </div>
  )
}
