import type { StatusTone } from '../../components/StatusBadge'
import type { WorkOrderStatus } from '../../types'

export const WORK_ORDER_STATUS_OPTIONS: { value: WorkOrderStatus; label: string }[] = [
  { value: 'DRAFT', label: 'Rascunho' },
  { value: 'RELEASED', label: 'Liberada' },
  { value: 'IN_PROGRESS', label: 'Em Andamento' },
  { value: 'COMPLETED', label: 'Concluída' },
  { value: 'CANCELLED', label: 'Cancelada' },
]

export const WORK_ORDER_STATUS_TONE: Record<WorkOrderStatus, StatusTone> = {
  DRAFT: 'neutral',
  RELEASED: 'info',
  IN_PROGRESS: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'danger',
}

export function workOrderStatusLabel(status: WorkOrderStatus): string {
  return WORK_ORDER_STATUS_OPTIONS.find((option) => option.value === status)?.label ?? status
}
