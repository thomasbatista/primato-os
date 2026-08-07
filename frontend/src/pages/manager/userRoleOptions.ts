import type { StatusTone } from '../../components/StatusBadge'
import type { UserRole } from '../../types'

export const USER_ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: 'MANAGER', label: 'Gestor' },
  { value: 'WORKER', label: 'Colaborador' },
]

export const USER_ROLE_TONE: Record<UserRole, StatusTone> = {
  MANAGER: 'info',
  WORKER: 'neutral',
}

export function userRoleLabel(role: UserRole): string {
  return USER_ROLE_OPTIONS.find((option) => option.value === role)?.label ?? role
}
