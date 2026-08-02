export type StatusTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

// Semantic status colors, kept separate from the brand palette — same reasoning as
// the red error banners elsewhere: these convey state, not brand identity.
const TONE_CLASSES: Record<StatusTone, string> = {
  neutral: 'bg-gray-100 text-gray-700',
  info: 'bg-blue-100 text-blue-700',
  success: 'bg-green-100 text-green-700',
  warning: 'bg-amber-100 text-amber-800',
  danger: 'bg-red-100 text-red-700',
}

interface StatusBadgeProps {
  label: string
  tone: StatusTone
}

export function StatusBadge({ label, tone }: StatusBadgeProps) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap ${TONE_CLASSES[tone]}`}>
      {label}
    </span>
  )
}
