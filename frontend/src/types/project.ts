import type { UserSummaryResponse } from './common'

export type ProjectStatus = 'PLANNING' | 'IN_PROGRESS' | 'PAUSED' | 'FINISHED' | 'CANCELLED'

export interface ProjectResponse {
  id: number
  name: string
  client: string
  address: string | null
  responsibleUser: UserSummaryResponse
  startDate: string | null
  expectedDeadline: string | null
  currentStage: string | null
  status: ProjectStatus
  notes: string | null
  createdAt: string
}

export interface ProjectSummaryResponse {
  id: number
  name: string
  client: string
}

export interface ProjectCreateRequest {
  name: string
  client: string
  address?: string | null
  responsibleUserId: number
  startDate?: string | null
  expectedDeadline?: string | null
  currentStage?: string | null
  notes?: string | null
}

export interface ProjectUpdateRequest {
  name: string
  client: string
  address?: string | null
  responsibleUserId: number
  startDate?: string | null
  expectedDeadline?: string | null
  currentStage?: string | null
  notes?: string | null
  status: ProjectStatus
}

export interface ProjectPhotoResponse {
  id: number
  url: string
  createdAt: string
}
