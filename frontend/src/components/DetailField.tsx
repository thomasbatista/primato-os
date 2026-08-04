import type { ReactNode } from 'react'

interface DetailFieldProps {
  label: string
  children: ReactNode
}

export function DetailField({ label, children }: DetailFieldProps) {
  return (
    <div>
      <dt className="text-xs font-medium text-gray-500">{label}</dt>
      <dd className="mt-0.5 text-sm text-foreground">{children || '—'}</dd>
    </div>
  )
}
