import type { UserSummaryResponse } from './common'
import type { ProjectSummaryResponse } from './project'
import type { WorkerSummaryResponse } from './worker'

export type WorkOrderStatus = 'DRAFT' | 'RELEASED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export interface WorkOrderResponse {
  id: number
  orderNumber: number
  project: ProjectSummaryResponse
  date: string
  responsibleUser: UserSummaryResponse
  stage: string
  location: string | null
  description: string
  dailyGoal: string | null
  plannedStartTime: string | null
  plannedEndTime: string | null
  materialsNeeded: string | null
  tools: string | null
  safetyGuidelines: string | null
  qualityCriteria: string | null
  notes: string | null
  status: WorkOrderStatus
  assignedWorkers: WorkerSummaryResponse[]
  createdAt: string
}

export interface WorkOrderSummaryResponse {
  id: number
  orderNumber: number
  stage: string
}

export interface WorkOrderCreateRequest {
  projectId: number
  date: string
  responsibleUserId: number
  stage: string
  location?: string | null
  description: string
  dailyGoal?: string | null
  plannedStartTime?: string | null
  plannedEndTime?: string | null
  materialsNeeded?: string | null
  tools?: string | null
  safetyGuidelines?: string | null
  qualityCriteria?: string | null
  notes?: string | null
  assignedWorkerIds?: number[]
}

export interface WorkOrderUpdateRequest {
  projectId: number
  date: string
  responsibleUserId: number
  stage: string
  location?: string | null
  description: string
  dailyGoal?: string | null
  plannedStartTime?: string | null
  plannedEndTime?: string | null
  materialsNeeded?: string | null
  tools?: string | null
  safetyGuidelines?: string | null
  qualityCriteria?: string | null
  notes?: string | null
  assignedWorkerIds?: number[]
}
