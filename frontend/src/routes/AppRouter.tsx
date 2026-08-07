import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { ProtectedRoute } from './ProtectedRoute'
import { ManagerLayout } from '../components/ManagerLayout'
import { WorkerLayout } from '../components/WorkerLayout'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { UnauthorizedPage } from '../pages/UnauthorizedPage'
import { ManagerDashboardPage } from '../pages/manager/ManagerDashboardPage'
import { ProjectListPage } from '../pages/manager/ProjectListPage'
import { ProjectFormPage } from '../pages/manager/ProjectFormPage'
import { WorkOrderListPage } from '../pages/manager/WorkOrderListPage'
import { WorkOrderDetailPage } from '../pages/manager/WorkOrderDetailPage'
import { WorkOrderFormPage } from '../pages/manager/WorkOrderFormPage'
import { DailyReportListPage as ManagerDailyReportListPage } from '../pages/manager/DailyReportListPage'
import { DailyReportDetailPage } from '../pages/manager/DailyReportDetailPage'
import { MaterialRequestListPage } from '../pages/manager/MaterialRequestListPage'
import { MaterialRequestDetailPage } from '../pages/manager/MaterialRequestDetailPage'
import { MaterialRequestFormPage } from '../pages/manager/MaterialRequestFormPage'
import { UserListPage } from '../pages/manager/UserListPage'
import { UserFormPage } from '../pages/manager/UserFormPage'
import { WorkerWorkOrderListPage } from '../pages/worker/WorkerWorkOrderListPage'
import { WorkerWorkOrderDetailPage } from '../pages/worker/WorkerWorkOrderDetailPage'
import { DailyReportListPage as WorkerDailyReportListPage } from '../pages/worker/DailyReportListPage'
import { DailyReportFormPage } from '../pages/worker/DailyReportFormPage'

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
          { path: 'daily-reports', element: <ManagerDailyReportListPage /> },
          { path: 'daily-reports/:id', element: <DailyReportDetailPage /> },
          { path: 'material-requests', element: <MaterialRequestListPage /> },
          { path: 'material-requests/new', element: <MaterialRequestFormPage /> },
          { path: 'material-requests/new/from-work-order/:workOrderId', element: <MaterialRequestFormPage /> },
          { path: 'material-requests/:id', element: <MaterialRequestDetailPage /> },
          { path: 'material-requests/:id/edit', element: <MaterialRequestFormPage /> },
          { path: 'users', element: <UserListPage /> },
          { path: 'users/new', element: <UserFormPage /> },
        ],
      },
    ],
  },
  {
    path: '/worker',
    element: <ProtectedRoute allowedRole="WORKER" />,
    children: [
      {
        element: <WorkerLayout />,
        children: [
          { index: true, element: <WorkerWorkOrderListPage /> },
          { path: 'work-orders/:id', element: <WorkerWorkOrderDetailPage /> },
          { path: 'daily-reports', element: <WorkerDailyReportListPage /> },
          { path: 'daily-reports/new/:workOrderId', element: <DailyReportFormPage /> },
          { path: 'daily-reports/:id/edit', element: <DailyReportFormPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])

export function AppRouter() {
  return <RouterProvider router={router} />
}
