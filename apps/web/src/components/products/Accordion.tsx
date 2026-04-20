'use client'

import { useState } from 'react'

type Props = {
  title: string
  content: string
}

export default function Accordion({ title, content }: Props) {
  const [open, setOpen] = useState(false)

  return (
    <div className="border-t border-zinc-100">
      <button
        onClick={() => setOpen((v) => !v)}
        className="flex w-full items-center justify-between py-3.5 text-left text-sm font-medium text-zinc-900 hover:text-zinc-600 transition-colors"
      >
        {title}
        <svg
          className={`h-4 w-4 text-zinc-400 transition-transform duration-200 ${open ? 'rotate-180' : ''}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      {open && (
        <div className="pb-4 text-sm text-zinc-500 leading-relaxed">
          {content}
        </div>
      )}
    </div>
  )
}
