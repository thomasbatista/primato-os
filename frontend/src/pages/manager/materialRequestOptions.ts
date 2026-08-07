import type { StatusTone } from '../../components/StatusBadge'
import type { MaterialRequestPriority, MaterialRequestStatus, MaterialRequestUnit } from '../../types'

export const MATERIAL_REQUEST_STATUS_OPTIONS: { value: MaterialRequestStatus; label: string }[] = [
  { value: 'DRAFT', label: 'Rascunho' },
  { value: 'REQUESTED', label: 'Solicitado' },
  { value: 'APPROVED', label: 'Aprovado' },
  { value: 'PURCHASED', label: 'Comprado' },
  { value: 'PARTIALLY_DELIVERED', label: 'Parcialmente Entregue' },
  { value: 'DELIVERED', label: 'Entregue' },
  { value: 'CANCELLED', label: 'Cancelado' },
]

export const MATERIAL_REQUEST_STATUS_TONE: Record<MaterialRequestStatus, StatusTone> = {
  DRAFT: 'neutral',
  REQUESTED: 'info',
  APPROVED: 'info',
  PURCHASED: 'warning',
  PARTIALLY_DELIVERED: 'warning',
  DELIVERED: 'success',
  CANCELLED: 'danger',
}

export function materialRequestStatusLabel(status: MaterialRequestStatus): string {
  return MATERIAL_REQUEST_STATUS_OPTIONS.find((option) => option.value === status)?.label ?? status
}

export const MATERIAL_REQUEST_PRIORITY_OPTIONS: { value: MaterialRequestPriority; label: string }[] = [
  { value: 'LOW', label: 'Baixa' },
  { value: 'MEDIUM', label: 'Média' },
  { value: 'HIGH', label: 'Alta' },
  { value: 'URGENT', label: 'Urgente' },
]

export const MATERIAL_REQUEST_PRIORITY_TONE: Record<MaterialRequestPriority, StatusTone> = {
  LOW: 'neutral',
  MEDIUM: 'info',
  HIGH: 'warning',
  URGENT: 'danger',
}

export function materialRequestPriorityLabel(priority: MaterialRequestPriority): string {
  return MATERIAL_REQUEST_PRIORITY_OPTIONS.find((option) => option.value === priority)?.label ?? priority
}

export const MATERIAL_REQUEST_UNIT_OPTIONS: { value: MaterialRequestUnit; label: string }[] = [
  { value: 'UNIT', label: 'Unidade' },
  { value: 'METER', label: 'Metro (m)' },
  { value: 'SQUARE_METER', label: 'Metro Quadrado (m²)' },
  { value: 'CUBIC_METER', label: 'Metro Cúbico (m³)' },
  { value: 'KILOGRAM', label: 'Quilograma (kg)' },
  { value: 'BAG', label: 'Saco' },
  { value: 'BOX', label: 'Caixa' },
  { value: 'SHEET', label: 'Chapa/Folha' },
  { value: 'BAR', label: 'Barra' },
  { value: 'ROLL', label: 'Rolo' },
  { value: 'LITER', label: 'Litro (L)' },
]

export function materialRequestUnitLabel(unit: MaterialRequestUnit): string {
  return MATERIAL_REQUEST_UNIT_OPTIONS.find((option) => option.value === unit)?.label ?? unit
}
