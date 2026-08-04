import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { ProtectedRoute } from './ProtectedRoute'
import { ManagerLayout } from '../components/ManagerLayout'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { UnauthorizedPage } from '../pages/UnauthorizedPage'
import { ManagerDashboardPage } from '../pages/manager/ManagerDashboardPage'
import { ProjectListPage } from '../pages/manager/ProjectListPage'
import { ProjectFormPage } from '../pages/manager/ProjectFormPage'
import { WorkOrderListPage } from '../pages/manager/WorkOrderListPage'
import { WorkOrderDetailPage } from '../pages/manager/WorkOrderDetailPage'
import { WorkOrderFormPage } from '../pages/manager/WorkOrderFormPage'
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
    children: [
      {
        element: <ManagerLayout />,
        children: [
          { index: true, element: <ManagerDashboardPage /> },
          { path: 'projects', element: <ProjectListPage /> },
          { path: 'projects/new', element: <ProjectFormPage /> },
          { path: 'projects/:id/edit', element: <ProjectFormPage /> },
          { path: 'work-orders', element: <WorkOrderListPage /> },
          { path: 'work-orders/new', element: <WorkOrderFormPage /> },
          { path: 'work-orders/:id', element: <WorkOrderDetailPage /> },
          { path: 'work-orders/:id/edit', element: <WorkOrderFormPage /> },
        ],
      },
    ],
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
