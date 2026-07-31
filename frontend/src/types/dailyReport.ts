import type { WorkerSummaryResponse } from './worker'
import type { WorkOrderSummaryResponse } from './workOrder'

export type DailyReportStatus = 'DRAFT' | 'FINALIZED'

export type DailyReportItemStatus =
  | 'EXECUTED'
  | 'PARTIALLY_EXECUTED'
  | 'NOT_EXECUTED'
  | 'NOT_APPLICABLE'

export interface DailyReportItemResponse {
  id: number
  activityDescription: string
  status: DailyReportItemStatus
  reason: string | null
  observation: string | null
  newExpectedDate: string | null
}

export interface DailyReportItemRequest {
  activityDescription: string
  status: DailyReportItemStatus
  reason?: string | null
  observation?: string | null
  newExpectedDate?: string | null
}

export interface DailyReportPhotoResponse {
  id: number
  url: string
  createdAt: string
}

export interface DailyReportResponse {
  id: number
  workOrder: WorkOrderSummaryResponse
  date: string
  filledByWorker: WorkerSummaryResponse
  teamPresent: WorkerSummaryResponse[]
  startTime: string | null
  endTime: string | null
  weatherCondition: string | null
  extraServicesExecuted: string | null
  problemsFound: string | null
  pendingIssuesGenerated: string | null
  materialsUsed: string | null
  materialsMissing: string | null
  forecastForNextDay: string | null
  notes: string | null
  status: DailyReportStatus
  items: DailyReportItemResponse[]
  photos: DailyReportPhotoResponse[]
  createdAt: string
}

export interface DailyReportSummaryResponse {
  id: number
  workOrder: WorkOrderSummaryResponse
  date: string
  filledByWorker: WorkerSummaryResponse
  status: DailyReportStatus
}

export interface DailyReportCreateRequest {
  workOrderId: number
  date: string
  teamPresentWorkerIds?: number[]
  startTime?: string | null
  endTime?: string | null
  weatherCondition?: string | null
  extraServicesExecuted?: string | null
  problemsFound?: string | null
  pendingIssuesGenerated?: string | null
  materialsUsed?: string | null
  materialsMissing?: string | null
  forecastForNextDay?: string | null
  notes?: string | null
  items?: DailyReportItemRequest[]
}

export interface DailyReportUpdateRequest {
  date: string
  teamPresentWorkerIds?: number[]
  startTime?: string | null
  endTime?: string | null
  weatherCondition?: string | null
  extraServicesExecuted?: string | null
  problemsFound?: string | null
  pendingIssuesGenerated?: string | null
  materialsUsed?: string | null
  materialsMissing?: string | null
  forecastForNextDay?: string | null
  notes?: string | null
  items?: DailyReportItemRequest[]
}
