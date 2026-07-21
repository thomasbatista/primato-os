package com.primatoos.backend.service;

import com.primatoos.backend.dto.dailyreport.DailyReportSummaryResponse;
import com.primatoos.backend.dto.dashboard.DashboardResponse;
import com.primatoos.backend.dto.dashboard.DashboardSection;
import com.primatoos.backend.dto.materialrequest.MaterialRequestSummaryResponse;
import com.primatoos.backend.dto.project.ProjectSummaryResponse;
import com.primatoos.backend.dto.workorder.WorkOrderSummaryResponse;
import com.primatoos.backend.mapper.DailyReportMapper;
import com.primatoos.backend.mapper.MaterialRequestMapper;
import com.primatoos.backend.mapper.ProjectMapper;
import com.primatoos.backend.mapper.WorkOrderMapper;
import com.primatoos.backend.model.DailyReport;
import com.primatoos.backend.model.DailyReportStatus;
import com.primatoos.backend.model.MaterialRequest;
import com.primatoos.backend.model.MaterialRequestStatus;
import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.ProjectStatus;
import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.model.WorkOrderStatus;
import com.primatoos.backend.repository.DailyReportRepository;
import com.primatoos.backend.repository.MaterialRequestRepository;
import com.primatoos.backend.repository.ProjectRepository;
import com.primatoos.backend.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int SHORT_LIST_SIZE = 5;

    // "today" must follow the Brazilian calendar day, not the server's own timezone (Render defaults to UTC)
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    private final WorkOrderRepository workOrderRepository;
    private final DailyReportRepository dailyReportRepository;
    private final MaterialRequestRepository materialRequestRepository;
    private final ProjectRepository projectRepository;
    private final WorkOrderMapper workOrderMapper;
    private final DailyReportMapper dailyReportMapper;
    private final MaterialRequestMapper materialRequestMapper;
    private final ProjectMapper projectMapper;

    public DashboardResponse getDashboard() {
        Pageable mostRecentFirst = PageRequest.of(0, SHORT_LIST_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable oldestFirst = PageRequest.of(0, SHORT_LIST_SIZE, Sort.by(Sort.Direction.ASC, "createdAt"));

        return new DashboardResponse(
                todayWorkOrders(mostRecentFirst),
                pendingDailyReports(oldestFirst),
                openMaterialRequests(mostRecentFirst),
                materialRequestsAwaitingDelivery(mostRecentFirst),
                activeProjects(mostRecentFirst)
        );
    }

    private DashboardSection<WorkOrderSummaryResponse> todayWorkOrders(Pageable pageable) {
        LocalDate today = LocalDate.now(BRAZIL_ZONE);
        Page<WorkOrder> page = workOrderRepository.findByDateAndStatusIn(today,
                List.of(WorkOrderStatus.RELEASED, WorkOrderStatus.IN_PROGRESS), pageable);

        return new DashboardSection<>(page.getTotalElements(),
                page.getContent().stream().map(workOrderMapper::toSummary).toList());
    }

    // oldest-first: surfaces reports that have been waiting longest, not the newest ones
    private DashboardSection<DailyReportSummaryResponse> pendingDailyReports(Pageable pageable) {
        Page<DailyReport> page = dailyReportRepository.findByStatus(DailyReportStatus.DRAFT, pageable);

        return new DashboardSection<>(page.getTotalElements(),
                page.getContent().stream().map(dailyReportMapper::toSummary).toList());
    }

    private DashboardSection<MaterialRequestSummaryResponse> openMaterialRequests(Pageable pageable) {
        Page<MaterialRequest> page = materialRequestRepository.findByStatusIn(
                List.of(MaterialRequestStatus.REQUESTED, MaterialRequestStatus.APPROVED), pageable);

        return new DashboardSection<>(page.getTotalElements(),
                page.getContent().stream().map(materialRequestMapper::toSummary).toList());
    }

    private DashboardSection<MaterialRequestSummaryResponse> materialRequestsAwaitingDelivery(Pageable pageable) {
        Page<MaterialRequest> page = materialRequestRepository.findByStatusIn(
                List.of(MaterialRequestStatus.PURCHASED, MaterialRequestStatus.PARTIALLY_DELIVERED), pageable);

        return new DashboardSection<>(page.getTotalElements(),
                page.getContent().stream().map(materialRequestMapper::toSummary).toList());
    }

    private DashboardSection<ProjectSummaryResponse> activeProjects(Pageable pageable) {
        Page<Project> page = projectRepository.findByStatus(ProjectStatus.IN_PROGRESS, pageable);

        return new DashboardSection<>(page.getTotalElements(),
                page.getContent().stream().map(projectMapper::toSummary).toList());
    }
}
