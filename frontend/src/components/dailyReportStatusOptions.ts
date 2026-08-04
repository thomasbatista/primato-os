import type { StatusTone } from './StatusBadge'
import type { DailyReportItemStatus, DailyReportStatus } from '../types'

// Lives in components/ (not pages/manager/ like projectStatusOptions and
// workOrderStatusOptions) because Daily Reports are the first entity in this app
// with a genuine manager+worker shared UI — both role trees need this mapping.

export const DAILY_REPORT_STATUS_OPTIONS: { value: DailyReportStatus; label: string }[] = [
  { value: 'DRAFT', label: 'Rascunho' },
  { value: 'FINALIZED', label: 'Finalizado' },
]

export const DAILY_REPORT_STATUS_TONE: Record<DailyReportStatus, StatusTone> = {
  DRAFT: 'neutral',
  FINALIZED: 'success',
}

export function dailyReportStatusLabel(status: DailyReportStatus): string {
  return DAILY_REPORT_STATUS_OPTIONS.find((option) => option.value === status)?.label ?? status
}

export const DAILY_REPORT_ITEM_STATUS_OPTIONS: { value: DailyReportItemStatus; label: string }[] = [
  { value: 'EXECUTED', label: 'Executado' },
  { value: 'PARTIALLY_EXECUTED', label: 'Parcialmente Executado' },
  { value: 'NOT_EXECUTED', label: 'Não Executado' },
  { value: 'NOT_APPLICABLE', label: 'Não Aplicável' },
]

export const DAILY_REPORT_ITEM_STATUS_TONE: Record<DailyReportItemStatus, StatusTone> = {
  EXECUTED: 'success',
  PARTIALLY_EXECUTED: 'warning',
  NOT_EXECUTED: 'danger',
  NOT_APPLICABLE: 'neutral',
}

export function dailyReportItemStatusLabel(status: DailyReportItemStatus): string {
  return DAILY_REPORT_ITEM_STATUS_OPTIONS.find((option) => option.value === status)?.label ?? status
}

export function itemRequiresDetails(status: DailyReportItemStatus | ''): boolean {
  return status === 'PARTIALLY_EXECUTED' || status === 'NOT_EXECUTED'
}
