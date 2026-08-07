import api from './api'
import type { Page, WorkerCreateRequest, WorkerResponse, WorkerUpdateRequest } from '../types'

// Bounded to a generous single page rather than paginated — same reasoning as
// getManagers(): this backs a multi-select in the work order form, not a list screen.
export async function getActiveWorkers(): Promise<WorkerResponse[]> {
  const response = await api.get<Page<WorkerResponse>>('/workers', { params: { active: true, size: 100 } })
  return response.data.content
}

interface GetWorkersParams {
  active?: boolean
  page?: number
  size?: number
}

export async function getWorkers(params: GetWorkersParams = {}): Promise<Page<WorkerResponse>> {
  const response = await api.get<Page<WorkerResponse>>('/workers', { params })
  return response.data
}

export async function getWorker(id: number): Promise<WorkerResponse> {
  const response = await api.get<WorkerResponse>(`/workers/${id}`)
  return response.data
}

export async function createWorker(request: WorkerCreateRequest): Promise<WorkerResponse> {
  const response = await api.post<WorkerResponse>('/workers', request)
  return response.data
}

export async function updateWorker(id: number, request: WorkerUpdateRequest): Promise<WorkerResponse> {
  const response = await api.put<WorkerResponse>(`/workers/${id}`, request)
  return response.data
}

export async function deactivateWorker(id: number): Promise<WorkerResponse> {
  const response = await api.patch<WorkerResponse>(`/workers/${id}/deactivate`)
  return response.data
}
