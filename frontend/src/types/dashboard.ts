import type { DailyReportSummaryResponse } from './dailyReport'
import type { MaterialRequestSummaryResponse } from './materialRequest'
import type { ProjectSummaryResponse } from './project'
import type { WorkOrderSummaryResponse } from './workOrder'

export interface DashboardSection<T> {
  count: number
  items: T[]
}

export interface DashboardResponse {
  todayWorkOrders: DashboardSection<WorkOrderSummaryResponse>
  pendingDailyReports: DashboardSection<DailyReportSummaryResponse>
  openMaterialRequests: DashboardSection<MaterialRequestSummaryResponse>
  materialRequestsAwaitingDelivery: DashboardSection<MaterialRequestSummaryResponse>
  activeProjects: DashboardSection<ProjectSummaryResponse>
}
