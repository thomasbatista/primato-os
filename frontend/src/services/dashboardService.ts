import api from './api'
import type { DashboardResponse } from '../types'

export async function getDashboard(): Promise<DashboardResponse> {
  const response = await api.get<DashboardResponse>('/dashboard')
  return response.data
}
