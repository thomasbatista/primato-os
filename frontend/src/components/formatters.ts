// Parsed manually rather than via `new Date(...)` — these are date-only/time-only ISO
// strings with no timezone component, and letting the Date constructor interpret them as
// UTC-midnight then reformatting in local time risks an off-by-one-day shift.
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

export function formatTime(isoTime: string | null): string | null {
  return isoTime ? isoTime.slice(0, 5) : null
}
