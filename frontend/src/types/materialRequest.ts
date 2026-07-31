import type { UserSummaryResponse } from './common'
import type { ProjectSummaryResponse } from './project'
import type { WorkOrderSummaryResponse } from './workOrder'

export type MaterialRequestStatus =
  | 'DRAFT'
  | 'REQUESTED'
  | 'APPROVED'
  | 'PURCHASED'
  | 'PARTIALLY_DELIVERED'
  | 'DELIVERED'
  | 'CANCELLED'

export type MaterialRequestPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export type MaterialRequestUnit =
  | 'UNIT'
  | 'METER'
  | 'SQUARE_METER'
  | 'CUBIC_METER'
  | 'KILOGRAM'
  | 'BAG'
  | 'BOX'
  | 'SHEET'
  | 'BAR'
  | 'ROLL'
  | 'LITER'

export interface MaterialRequestItemResponse {
  id: number
  name: string
  description: string | null
  quantity: number
  unit: MaterialRequestUnit
  brand: string | null
  photoReference: string | null
  notes: string | null
  quantityDelivered: number
}

export interface MaterialRequestItemRequest {
  name: string
  description?: string | null
  quantity: number
  unit: MaterialRequestUnit
  brand?: string | null
  photoReference?: string | null
  notes?: string | null
}

export interface MaterialRequestResponse {
  id: number
  requestNumber: number
  project: ProjectSummaryResponse
  workOrder: WorkOrderSummaryResponse | null
  requestDate: string
  neededByDate: string | null
  requester: UserSummaryResponse
  priority: MaterialRequestPriority
  justification: string | null
  notes: string | null
  deliveryLocation: string | null
  status: MaterialRequestStatus
  items: MaterialRequestItemResponse[]
  createdAt: string
}

export interface MaterialRequestSummaryResponse {
  id: number
  requestNumber: number
  project: ProjectSummaryResponse
  priority: MaterialRequestPriority
  status: MaterialRequestStatus
}

export interface MaterialRequestCreateRequest {
  projectId: number
  workOrderId?: number | null
  requestDate: string
  neededByDate?: string | null
  requesterId: number
  priority: MaterialRequestPriority
  justification?: string | null
  notes?: string | null
  deliveryLocation?: string | null
  items: MaterialRequestItemRequest[]
}

export interface MaterialRequestUpdateRequest {
  projectId: number
  workOrderId?: number | null
  requestDate: string
  neededByDate?: string | null
  requesterId: number
  priority: MaterialRequestPriority
  justification?: string | null
  notes?: string | null
  deliveryLocation?: string | null
  items: MaterialRequestItemRequest[]
}

export interface MaterialRequestFromWorkOrderRequest {
  neededByDate?: string | null
  priority: MaterialRequestPriority
  justification?: string | null
  notes?: string | null
  deliveryLocation?: string | null
  items: MaterialRequestItemRequest[]
}

export interface DeliveryItemRequest {
  materialRequestItemId: number
  quantityDelivered: number
}

export interface RegisterDeliveryRequest {
  items: DeliveryItemRequest[]
}
