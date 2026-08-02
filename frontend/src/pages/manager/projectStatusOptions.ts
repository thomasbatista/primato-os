import type { StatusTone } from '../../components/StatusBadge'
import type { ProjectStatus } from '../../types'

export const PROJECT_STATUS_OPTIONS: { value: ProjectStatus; label: string }[] = [
  { value: 'PLANNING', label: 'Planejamento' },
  { value: 'IN_PROGRESS', label: 'Em Andamento' },
  { value: 'PAUSED', label: 'Pausada' },
  { value: 'FINISHED', label: 'Concluída' },
  { value: 'CANCELLED', label: 'Cancelada' },
]

export const PROJECT_STATUS_TONE: Record<ProjectStatus, StatusTone> = {
  PLANNING: 'neutral',
  IN_PROGRESS: 'info',
  PAUSED: 'warning',
  FINISHED: 'success',
  CANCELLED: 'danger',
}

export function projectStatusLabel(status: ProjectStatus): string {
  return PROJECT_STATUS_OPTIONS.find((option) => option.value === status)?.label ?? status
}
