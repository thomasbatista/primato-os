import type { AxiosProgressEvent } from 'axios'
import api from './api'
import type {
  DailyReportCreateRequest,
  DailyReportPhotoResponse,
  DailyReportResponse,
  DailyReportUpdateRequest,
  Page,
} from '../types'

interface GetByWorkOrderParams {
  workOrderId: number
  page?: number
  size?: number
}

// Manager-side listing — the backend requires workOrderId, there's no "all reports" view.
export async function getDailyReportsByWorkOrder(params: GetByWorkOrderParams): Promise<Page<DailyReportResponse>> {
  const response = await api.get<Page<DailyReportResponse>>('/daily-reports', { params })
  return response.data
}

interface GetMyReportsParams {
  workOrderId?: number
  page?: number
  size?: number
}

export async function getMyDailyReports(params: GetMyReportsParams = {}): Promise<Page<DailyReportResponse>> {
  const response = await api.get<Page<DailyReportResponse>>('/daily-reports/mine', { params })
  return response.data
}

export async function getDailyReport(id: number): Promise<DailyReportResponse> {
  const response = await api.get<DailyReportResponse>(`/daily-reports/${id}`)
  return response.data
}

export async function createDailyReport(request: DailyReportCreateRequest): Promise<DailyReportResponse> {
  const response = await api.post<DailyReportResponse>('/daily-reports', request)
  return response.data
}

export async function updateDailyReport(id: number, request: DailyReportUpdateRequest): Promise<DailyReportResponse> {
  const response = await api.put<DailyReportResponse>(`/daily-reports/${id}`, request)
  return response.data
}

export async function finalizeDailyReport(id: number): Promise<DailyReportResponse> {
  const response = await api.patch<DailyReportResponse>(`/daily-reports/${id}/finalize`)
  return response.data
}

export async function reopenDailyReport(id: number): Promise<DailyReportResponse> {
  const response = await api.patch<DailyReportResponse>(`/daily-reports/${id}/reopen`)
  return response.data
}

export async function uploadDailyReportPhoto(
  id: number,
  file: File,
  onProgress?: (percent: number) => void,
): Promise<DailyReportPhotoResponse> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await api.post<DailyReportPhotoResponse>(`/daily-reports/${id}/photos`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (event: AxiosProgressEvent) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    },
  })
  return response.data
}

export async function deleteDailyReportPhoto(id: number, photoId: number): Promise<void> {
  await api.delete(`/daily-reports/${id}/photos/${photoId}`)
}
