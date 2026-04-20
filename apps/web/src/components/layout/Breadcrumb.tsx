import Link from 'next/link'

type BreadcrumbItem = {
  label: string
  href?: string
}

type Props = {
  items: BreadcrumbItem[]
}

export default function Breadcrumb({ items }: Props) {
  return (
    <nav aria-label="breadcrumb" className="mb-6 flex items-center gap-1.5 text-sm text-zinc-500">
      {items.map((item, index) => (
        <span key={index} className="flex items-center gap-1.5">
          {index > 0 && <span className="text-zinc-300">›</span>}
          {item.href ? (
            <Link href={item.href} className="hover:text-zinc-900 transition-colors">
              {item.label}
            </Link>
          ) : (
            <span className="text-zinc-900 font-medium">{item.label}</span>
          )}
        </span>
      ))}
    </nav>
  )
}
