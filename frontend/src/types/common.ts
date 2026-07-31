export interface ErrorResponse {
  message: string
  status: number
  timestamp: string
}

export interface UserSummaryResponse {
  id: number
  name: string
  email: string
}

// Mirrors Spring Data's default Page<T> JSON shape. Spring itself warns this
// structure isn't guaranteed stable across versions (see backend startup logs) —
// if the backend switches to PagedModel, this type needs to follow.
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
}
