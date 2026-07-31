import type { UserSummaryResponse } from './common'

export interface WorkerResponse {
  id: number
  name: string
  function: string | null
  phone: string | null
  active: boolean
  user: UserSummaryResponse | null
  createdAt: string
}

export interface WorkerSummaryResponse {
  id: number
  name: string
  function: string | null
}

export interface WorkerCreateRequest {
  name: string
  function?: string | null
  phone?: string | null
  userId?: number | null
}

export interface WorkerUpdateRequest {
  name: string
  function?: string | null
  phone?: string | null
  userId?: number | null
}
