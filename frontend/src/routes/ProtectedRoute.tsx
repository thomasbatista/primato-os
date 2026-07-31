import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import type { UserRole } from '../types'

interface ProtectedRouteProps {
  allowedRole: UserRole
}

export function ProtectedRoute({ allowedRole }: ProtectedRouteProps) {
  const { user, isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (user?.role !== allowedRole) {
    return <Navigate to="/unauthorized" replace />
  }

  return <Outlet />
}
