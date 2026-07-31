import type { UserRole } from './user'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
}

export interface AuthMeResponse {
  name: string
  email: string
  role: UserRole
}
