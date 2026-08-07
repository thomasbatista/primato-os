import api from './api'
import type {
  MaterialRequestCreateRequest,
  MaterialRequestFromWorkOrderRequest,
  MaterialRequestResponse,
  MaterialRequestStatus,
  MaterialRequestUpdateRequest,
  Page,
  RegisterDeliveryRequest,
} from '../types'

interface GetMaterialRequestsParams {
  projectId?: number
  workOrderId?: number
  status?: MaterialRequestStatus
  page?: number
  size?: number
}

export async function getMaterialRequests(
  params: GetMaterialRequestsParams = {},
): Promise<Page<MaterialRequestResponse>> {
  const response = await api.get<Page<MaterialRequestResponse>>('/material-requests', { params })
  return response.data
}

export async function getMaterialRequest(id: number): Promise<MaterialRequestResponse> {
  const response = await api.get<MaterialRequestResponse>(`/material-requests/${id}`)
  return response.data
}

export async function createMaterialRequest(
  request: MaterialRequestCreateRequest,
): Promise<MaterialRequestResponse> {
  const response = await api.post<MaterialRequestResponse>('/material-requests', request)
  return response.data
}

export async function createMaterialRequestFromWorkOrder(
  workOrderId: number,
  request: MaterialRequestFromWorkOrderRequest,
): Promise<MaterialRequestResponse> {
  const response = await api.post<MaterialRequestResponse>(
    `/material-requests/from-work-order/${workOrderId}`,
    request,
  )
  return response.data
}

export async function updateMaterialRequest(
  id: number,
  request: MaterialRequestUpdateRequest,
): Promise<MaterialRequestResponse> {
  const response = await api.put<MaterialRequestResponse>(`/material-requests/${id}`, request)
  return response.data
}

export async function submitMaterialRequest(id: number): Promise<MaterialRequestResponse> {
  const response = await api.patch<MaterialRequestResponse>(`/material-requests/${id}/submit`)
  return response.data
}

export async function approveMaterialRequest(id: number): Promise<MaterialRequestResponse> {
  const response = await api.patch<MaterialRequestResponse>(`/material-requests/${id}/approve`)
  return response.data
}

export async function markMaterialRequestPurchased(id: number): Promise<MaterialRequestResponse> {
  const response = await api.patch<MaterialRequestResponse>(`/material-requests/${id}/purchase`)
  return response.data
}

export async function cancelMaterialRequest(id: number): Promise<MaterialRequestResponse> {
  const response = await api.patch<MaterialRequestResponse>(`/material-requests/${id}/cancel`)
  return response.data
}

export async function registerMaterialRequestDelivery(
  id: number,
  request: RegisterDeliveryRequest,
): Promise<MaterialRequestResponse> {
  const response = await api.patch<MaterialRequestResponse>(`/material-requests/${id}/deliveries`, request)
  return response.data
}

export async function duplicateMaterialRequest(id: number): Promise<MaterialRequestResponse> {
  const response = await api.post<MaterialRequestResponse>(`/material-requests/${id}/duplicate`)
  return response.data
}

export async function downloadMaterialRequestPdf(id: number, requestNumber: number): Promise<void> {
  const response = await api.get<Blob>(`/material-requests/${id}/pdf`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)

  const link = document.createElement('a')
  link.href = url
  link.download = `pedido-${requestNumber}.pdf`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
