export type UserRole = 'MANAGER' | 'WORKER'

export interface UserResponse {
  id: number
  name: string
  email: string
  role: UserRole
  createdAt: string
}

export interface UserCreateRequest {
  name: string
  email: string
  password: string
  role: UserRole
}
