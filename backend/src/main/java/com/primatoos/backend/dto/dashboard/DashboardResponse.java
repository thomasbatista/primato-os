package com.primatoos.backend.dto.dashboard;

import com.primatoos.backend.dto.dailyreport.DailyReportSummaryResponse;
import com.primatoos.backend.dto.materialrequest.MaterialRequestSummaryResponse;
import com.primatoos.backend.dto.project.ProjectSummaryResponse;
import com.primatoos.backend.dto.workorder.WorkOrderSummaryResponse;

public record DashboardResponse(
        DashboardSection<WorkOrderSummaryResponse> todayWorkOrders,
        DashboardSection<DailyReportSummaryResponse> pendingDailyReports,
        DashboardSection<MaterialRequestSummaryResponse> openMaterialRequests,
        DashboardSection<MaterialRequestSummaryResponse> materialRequestsAwaitingDelivery,
        DashboardSection<ProjectSummaryResponse> activeProjects
) {
}
