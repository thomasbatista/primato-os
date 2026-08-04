import api from './api'
import type { Page, WorkerResponse } from '../types'

// Bounded to a generous single page rather than paginated — same reasoning as
// getManagers(): this backs a multi-select in the work order form, not a list screen.
export async function getActiveWorkers(): Promise<WorkerResponse[]> {
  const response = await api.get<Page<WorkerResponse>>('/workers', { params: { active: true, size: 100 } })
  return response.data.content
}
