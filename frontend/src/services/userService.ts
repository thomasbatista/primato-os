import api from './api'
import type { Page, UserCreateRequest, UserResponse, UserRole } from '../types'

// Bounded to a generous single page rather than paginated — this backs a role picker
// dropdown, not a user list screen, and construction companies realistically have a
// handful to a few dozen managers, not hundreds.
export async function getManagers(): Promise<UserResponse[]> {
  const response = await api.get<Page<UserResponse>>('/users', { params: { role: 'MANAGER', size: 100 } })
  return response.data.content
}

interface GetUsersParams {
  role?: UserRole
  unlinked?: boolean
  page?: number
  size?: number
}

export async function getUsers(params: GetUsersParams = {}): Promise<Page<UserResponse>> {
  const response = await api.get<Page<UserResponse>>('/users', { params })
  return response.data
}

// Same bounded-single-page reasoning as getManagers() — backs the "link to a login"
// picker in the Worker form, not a list screen.
export async function getUnlinkedWorkerUsers(): Promise<UserResponse[]> {
  const response = await api.get<Page<UserResponse>>('/users', {
    params: { role: 'WORKER', unlinked: true, size: 100 },
  })
  return response.data.content
}

export async function createUser(request: UserCreateRequest): Promise<UserResponse> {
  const response = await api.post<UserResponse>('/users', request)
  return response.data
}
