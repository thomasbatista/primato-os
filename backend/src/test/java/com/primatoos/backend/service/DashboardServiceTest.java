package com.primatoos.backend.service;

import com.primatoos.backend.dto.dashboard.DashboardResponse;
import com.primatoos.backend.mapper.DailyReportMapper;
import com.primatoos.backend.mapper.MaterialRequestMapper;
import com.primatoos.backend.mapper.ProjectMapper;
import com.primatoos.backend.mapper.UserMapper;
import com.primatoos.backend.mapper.WorkOrderMapper;
import com.primatoos.backend.mapper.WorkerMapper;
import com.primatoos.backend.model.DailyReport;
import com.primatoos.backend.model.DailyReportStatus;
import com.primatoos.backend.model.MaterialRequest;
import com.primatoos.backend.model.MaterialRequestPriority;
import com.primatoos.backend.model.MaterialRequestStatus;
import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.ProjectStatus;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.model.WorkOrderStatus;
import com.primatoos.backend.model.Worker;
import com.primatoos.backend.repository.DailyReportRepository;
import com.primatoos.backend.repository.MaterialRequestRepository;
import com.primatoos.backend.repository.ProjectRepository;
import com.primatoos.backend.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private DailyReportRepository dailyReportRepository;

    @Mock
    private MaterialRequestRepository materialRequestRepository;

    @Mock
    private ProjectRepository projectRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapper();
        WorkerMapper workerMapper = new WorkerMapper(userMapper);
        ProjectMapper projectMapper = new ProjectMapper(userMapper);
        WorkOrderMapper workOrderMapper = new WorkOrderMapper(userMapper, workerMapper, projectMapper);
        DailyReportMapper dailyReportMapper = new DailyReportMapper(workerMapper, workOrderMapper);
        MaterialRequestMapper materialRequestMapper =
                new MaterialRequestMapper(userMapper, projectMapper, workOrderMapper);

        dashboardService = new DashboardService(workOrderRepository, dailyReportRepository,
                materialRequestRepository, projectRepository, workOrderMapper, dailyReportMapper,
                materialRequestMapper, projectMapper);
    }

    private Project aProject() {
        return Project.builder().id(1L).name("Obra Dashboard").client("Cliente Dashboard").build();
    }

    private WorkOrder aWorkOrder() {
        return WorkOrder.builder().id(10L).orderNumber(1L).project(aProject()).stage("Fundação")
                .description("Concretar").status(WorkOrderStatus.IN_PROGRESS).build();
    }

    private Worker aWorker() {
        User user = User.builder().id(20L).name("Colaborador").email("worker@primatoos.test").password("hash")
                .role(UserRole.WORKER).build();
        return Worker.builder().id(21L).name("Colaborador").user(user).build();
    }

    @Test
    void shouldReturnCorrectCountsAndShortLists_forEachSection() {
        WorkOrder workOrder = aWorkOrder();
        DailyReport dailyReport = DailyReport.builder()
                .id(30L).workOrder(workOrder).date(LocalDate.now()).filledByWorker(aWorker())
                .status(DailyReportStatus.DRAFT).build();
        MaterialRequest openRequest = MaterialRequest.builder()
                .id(40L).requestNumber(1L).project(aProject()).requestDate(LocalDate.now())
                .priority(MaterialRequestPriority.HIGH).status(MaterialRequestStatus.REQUESTED)
                .build();
        MaterialRequest awaitingRequest = MaterialRequest.builder()
                .id(41L).requestNumber(2L).project(aProject()).requestDate(LocalDate.now())
                .priority(MaterialRequestPriority.URGENT).status(MaterialRequestStatus.PURCHASED)
                .build();
        Project activeProject = aProject();

        // totals are deliberately > the page size (5): Spring Data's PageImpl recalculates
        // totalElements down to offset+content.size() when offset+pageSize > total, since that
        // combination would otherwise be self-contradictory for a real paged query
        given(workOrderRepository.findByDateAndStatusIn(any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(workOrder), pageableOf(), 7));
        given(dailyReportRepository.findByStatus(eq(DailyReportStatus.DRAFT), any()))
                .willReturn(new PageImpl<>(List.of(dailyReport), pageableOf(), 8));
        given(materialRequestRepository.findByStatusIn(
                eq(List.of(MaterialRequestStatus.REQUESTED, MaterialRequestStatus.APPROVED)), any()))
                .willReturn(new PageImpl<>(List.of(openRequest), pageableOf(), 6));
        given(materialRequestRepository.findByStatusIn(
                eq(List.of(MaterialRequestStatus.PURCHASED, MaterialRequestStatus.PARTIALLY_DELIVERED)), any()))
                .willReturn(new PageImpl<>(List.of(awaitingRequest), pageableOf(), 10));
        given(projectRepository.findByStatus(eq(ProjectStatus.IN_PROGRESS), any()))
                .willReturn(new PageImpl<>(List.of(activeProject), pageableOf(), 9));

        DashboardResponse response = dashboardService.getDashboard();

        assertThat(response.todayWorkOrders().count()).isEqualTo(7);
        assertThat(response.todayWorkOrders().items()).hasSize(1);
        assertThat(response.todayWorkOrders().items().get(0).id()).isEqualTo(10L);

        assertThat(response.pendingDailyReports().count()).isEqualTo(8);
        assertThat(response.pendingDailyReports().items().get(0).id()).isEqualTo(30L);

        assertThat(response.openMaterialRequests().count()).isEqualTo(6);
        assertThat(response.openMaterialRequests().items().get(0).id()).isEqualTo(40L);

        assertThat(response.materialRequestsAwaitingDelivery().count()).isEqualTo(10);
        assertThat(response.materialRequestsAwaitingDelivery().items().get(0).id()).isEqualTo(41L);

        assertThat(response.activeProjects().count()).isEqualTo(9);
        assertThat(response.activeProjects().items().get(0).id()).isEqualTo(1L);
    }

    @Test
    void shouldQueryTodayWorkOrders_usingBrazilianCalendarDate() {
        stubAllRepositoriesWithEmptyPages();

        dashboardService.getDashboard();

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(workOrderRepository).findByDateAndStatusIn(dateCaptor.capture(), any(), any());

        assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
    }

    @Test
    void shouldQueryTodayWorkOrders_withReleasedAndInProgressStatuses() {
        stubAllRepositoriesWithEmptyPages();

        dashboardService.getDashboard();

        verify(workOrderRepository).findByDateAndStatusIn(any(),
                eq(List.of(WorkOrderStatus.RELEASED, WorkOrderStatus.IN_PROGRESS)), any());
    }

    @Test
    void shouldSortPendingDailyReportsOldestFirst_andOtherSectionsMostRecentFirst() {
        stubAllRepositoriesWithEmptyPages();

        dashboardService.getDashboard();

        assertThat(sortDirectionCapturedFrom(pageable ->
                verify(dailyReportRepository).findByStatus(eq(DailyReportStatus.DRAFT), pageable.capture())))
                .isEqualTo(Sort.Direction.ASC);

        assertThat(sortDirectionCapturedFrom(pageable ->
                verify(workOrderRepository).findByDateAndStatusIn(any(), any(), pageable.capture())))
                .isEqualTo(Sort.Direction.DESC);

        assertThat(sortDirectionCapturedFrom(pageable ->
                verify(projectRepository).findByStatus(eq(ProjectStatus.IN_PROGRESS), pageable.capture())))
                .isEqualTo(Sort.Direction.DESC);
    }

    private Sort.Direction sortDirectionCapturedFrom(Consumer<ArgumentCaptor<Pageable>> verification) {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verification.accept(captor);
        return captor.getValue().getSort().getOrderFor("createdAt").getDirection();
    }

    private void stubAllRepositoriesWithEmptyPages() {
        given(workOrderRepository.findByDateAndStatusIn(any(), any(), any()))
                .willReturn(new PageImpl<>(List.of()));
        given(dailyReportRepository.findByStatus(any(), any())).willReturn(new PageImpl<>(List.of()));
        given(materialRequestRepository.findByStatusIn(any(), any())).willReturn(new PageImpl<>(List.of()));
        given(projectRepository.findByStatus(any(), any())).willReturn(new PageImpl<>(List.of()));
    }

    private Pageable pageableOf() {
        return org.springframework.data.domain.PageRequest.of(0, 5);
    }
}
