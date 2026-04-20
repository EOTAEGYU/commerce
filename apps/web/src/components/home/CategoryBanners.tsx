import Link from 'next/link'
import type { components } from '@/types/api'

type CategoryResponse = components['schemas']['CategoryResponse']

type Props = {
  categories: CategoryResponse[]
}

const BG_CLASSES = [
  'bg-zinc-200',
  'bg-stone-200',
  'bg-zinc-300',
  'bg-slate-200',
]

export default function CategoryBanners({ categories }: Props) {
  const topCategories = categories.slice(0, 4)

  return (
    <div className="grid grid-cols-4 gap-4">
      {topCategories.map((cat, index) => (
        <Link
          key={cat.id}
          href={`/categories/${cat.id}`}
          className="group relative overflow-hidden"
          style={{ aspectRatio: '4/3' }}
        >
          {/* 배경 placeholder */}
          <div
            className={`absolute inset-0 ${BG_CLASSES[index % BG_CLASSES.length]} transition-transform duration-500 group-hover:scale-105 bg-[repeating-linear-gradient(45deg,transparent,transparent_12px,rgba(0,0,0,0.04)_12px,rgba(0,0,0,0.04)_13px)]`}
          />

          {/* 오버레이 */}
          <div className="absolute inset-0 bg-black/0 transition-colors duration-300 group-hover:bg-black/10" />

          {/* 캡션 */}
          <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/50 to-transparent px-4 py-4">
            <p className="text-xs font-mono uppercase tracking-widest text-white/70">Shop</p>
            <h3 className="text-lg font-bold text-white">{cat.name}</h3>
          </div>
        </Link>
      ))}
    </div>
  )
}
