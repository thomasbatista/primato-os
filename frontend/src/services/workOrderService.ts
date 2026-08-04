import api from './api'
import type { Page, WorkOrderCreateRequest, WorkOrderResponse, WorkOrderStatus, WorkOrderUpdateRequest } from '../types'

interface GetWorkOrdersParams {
  projectId?: number
  status?: WorkOrderStatus
  page?: number
  size?: number
}

export async function getWorkOrders(params: GetWorkOrdersParams = {}): Promise<Page<WorkOrderResponse>> {
  const response = await api.get<Page<WorkOrderResponse>>('/work-orders', { params })
  return response.data
}

export async function getWorkOrder(id: number): Promise<WorkOrderResponse> {
  const response = await api.get<WorkOrderResponse>(`/work-orders/${id}`)
  return response.data
}

export async function createWorkOrder(request: WorkOrderCreateRequest): Promise<WorkOrderResponse> {
  const response = await api.post<WorkOrderResponse>('/work-orders', request)
  return response.data
}

export async function updateWorkOrder(id: number, request: WorkOrderUpdateRequest): Promise<WorkOrderResponse> {
  const response = await api.put<WorkOrderResponse>(`/work-orders/${id}`, request)
  return response.data
}

export async function releaseWorkOrder(id: number): Promise<WorkOrderResponse> {
  const response = await api.patch<WorkOrderResponse>(`/work-orders/${id}/release`)
  return response.data
}

export async function startWorkOrder(id: number): Promise<WorkOrderResponse> {
  const response = await api.patch<WorkOrderResponse>(`/work-orders/${id}/start`)
  return response.data
}

export async function completeWorkOrder(id: number): Promise<WorkOrderResponse> {
  const response = await api.patch<WorkOrderResponse>(`/work-orders/${id}/complete`)
  return response.data
}

export async function cancelWorkOrder(id: number): Promise<WorkOrderResponse> {
  const response = await api.patch<WorkOrderResponse>(`/work-orders/${id}/cancel`)
  return response.data
}

export async function duplicateWorkOrder(id: number): Promise<WorkOrderResponse> {
  const response = await api.post<WorkOrderResponse>(`/work-orders/${id}/duplicate`)
  return response.data
}

// The endpoint needs the Authorization header, so a plain <a href> can't hit it directly —
// fetch as a blob through the shared api client (which attaches the token), then trigger
// the browser's save dialog via a throwaway object URL.
export async function downloadWorkOrderPdf(id: number, orderNumber: number): Promise<void> {
  const response = await api.get<Blob>(`/work-orders/${id}/pdf`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)

  const link = document.createElement('a')
  link.href = url
  link.download = `os-${orderNumber}.pdf`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
