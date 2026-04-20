type Props = {
  current: 1 | 2 | 3
}

const STEPS = [
  { step: 1, label: 'CART' },
  { step: 2, label: 'CHECKOUT' },
  { step: 3, label: 'PAYMENT' },
] as const

export default function StepIndicator({ current }: Props) {
  return (
    <div className="flex items-center justify-center gap-2 py-6">
      {STEPS.map(({ step, label }, idx) => (
        <div key={step} className="flex items-center gap-2">
          <div className="flex items-center gap-2">
            <span
              className={`inline-flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold ${
                current === step
                  ? 'bg-zinc-900 text-white'
                  : 'border border-zinc-300 text-zinc-400'
              }`}
            >
              {step}
            </span>
            <span
              className={`text-xs font-semibold tracking-widest uppercase ${
                current === step ? 'text-zinc-900' : 'text-zinc-400'
              }`}
            >
              {label}
            </span>
          </div>
          {idx < STEPS.length - 1 && (
            <span className="text-zinc-300">›</span>
          )}
        </div>
      ))}
    </div>
  )
}
