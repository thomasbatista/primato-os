import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { ProtectedRoute } from './ProtectedRoute'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { UnauthorizedPage } from '../pages/UnauthorizedPage'
import { ManagerDashboardPage } from '../pages/manager/ManagerDashboardPage'
import { WorkerHomePage } from '../pages/worker/WorkerHomePage'

function RootRedirect() {
  const { user, isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Navigate to={user?.role === 'MANAGER' ? '/manager' : '/worker'} replace />
}

const router = createBrowserRouter([
  { path: '/', element: <RootRedirect /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/unauthorized', element: <UnauthorizedPage /> },
  {
    path: '/manager',
    element: <ProtectedRoute allowedRole="MANAGER" />,
    children: [{ index: true, element: <ManagerDashboardPage /> }],
  },
  {
    path: '/worker',
    element: <ProtectedRoute allowedRole="WORKER" />,
    children: [{ index: true, element: <WorkerHomePage /> }],
  },
  { path: '*', element: <NotFoundPage /> },
])

export function AppRouter() {
  return <RouterProvider router={router} />
}
