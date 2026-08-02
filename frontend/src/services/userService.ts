import api from './api'
import type { Page, UserResponse } from '../types'

// Bounded to a generous single page rather than paginated — this backs a role picker
// dropdown, not a user list screen, and construction companies realistically have a
// handful to a few dozen managers, not hundreds.
export async function getManagers(): Promise<UserResponse[]> {
  const response = await api.get<Page<UserResponse>>('/users', { params: { role: 'MANAGER', size: 100 } })
  return response.data.content
}
