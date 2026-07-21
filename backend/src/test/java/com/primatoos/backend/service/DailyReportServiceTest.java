package com.primatoos.backend.service;

import com.primatoos.backend.dto.dailyreport.DailyReportCreateRequest;
import com.primatoos.backend.dto.dailyreport.DailyReportItemRequest;
import com.primatoos.backend.dto.dailyreport.DailyReportResponse;
import com.primatoos.backend.dto.dailyreport.DailyReportUpdateRequest;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.ForbiddenOperationException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.mapper.DailyReportMapper;
import com.primatoos.backend.mapper.ProjectMapper;
import com.primatoos.backend.mapper.UserMapper;
import com.primatoos.backend.mapper.WorkOrderMapper;
import com.primatoos.backend.mapper.WorkerMapper;
import com.primatoos.backend.model.DailyReport;
import com.primatoos.backend.model.DailyReportItemStatus;
import com.primatoos.backend.model.DailyReportStatus;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.model.WorkOrderStatus;
import com.primatoos.backend.model.Worker;
import com.primatoos.backend.repository.DailyReportRepository;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.repository.WorkOrderRepository;
import com.primatoos.backend.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyReportServiceTest {

    @Mock
    private DailyReportRepository dailyReportRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkerRepository workerRepository;

    private DailyReportService dailyReportService;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapper();
        WorkerMapper workerMapper = new WorkerMapper(userMapper);
        WorkOrderMapper workOrderMapper = new WorkOrderMapper(userMapper, workerMapper, new ProjectMapper(userMapper));
        dailyReportService = new DailyReportService(dailyReportRepository, workOrderRepository, userRepository,
                workerRepository, new DailyReportMapper(workerMapper, workOrderMapper));
    }

    private User aWorkerUser(Long id, String email) {
        return User.builder().id(id).name("Worker").email(email).password("hash").role(UserRole.WORKER).build();
    }

    private Worker aWorker(Long id, User user) {
        return Worker.builder().id(id).name("Colaborador").user(user).build();
    }

    private WorkOrder aWorkOrder(WorkOrderStatus status, Set<Worker> assignedWorkers) {
        return WorkOrder.builder()
                .id(500L)
                .orderNumber(1L)
                .stage("Fundação")
                .description("Concretar fundação")
                .status(status)
                .assignedWorkers(assignedWorkers)
                .build();
    }

    private DailyReportCreateRequest validCreateRequest(Long workOrderId, List<DailyReportItemRequest> items) {
        return new DailyReportCreateRequest(workOrderId, LocalDate.of(2026, 8, 1), null, null, null,
                "Ensolarado", null, null, null, null, null, null, null, items);
    }

    private DailyReportUpdateRequest validUpdateRequest(List<DailyReportItemRequest> items) {
        return new DailyReportUpdateRequest(LocalDate.of(2026, 8, 2), null, null, null,
                "Chuvoso", null, null, null, null, null, null, null, items);
    }

    @Test
    void shouldCreateDailyReport_whenWorkerIsAssignedToWorkOrder() {
        User user = aWorkerUser(10L, "worker@primatoos.test");
        Worker worker = aWorker(1L, user);
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of(worker));

        given(workOrderRepository.findById(500L)).willReturn(Optional.of(workOrder));
        given(userRepository.findByEmail("worker@primatoos.test")).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(10L)).willReturn(Optional.of(worker));
        given(dailyReportRepository.save(any(DailyReport.class))).willAnswer(invocation -> invocation.getArgument(0));

        DailyReportResponse response = dailyReportService.create("worker@primatoos.test",
                validCreateRequest(500L, null));

        assertThat(response.status()).isEqualTo(DailyReportStatus.DRAFT);
        assertThat(response.filledByWorker().id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowForbiddenOperationException_whenWorkerIsNotAssignedToWorkOrder() {
        User user = aWorkerUser(10L, "outsider@primatoos.test");
        Worker outsider = aWorker(2L, user);
        Worker assignedWorker = aWorker(1L, aWorkerUser(11L, "assigned@primatoos.test"));
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of(assignedWorker));

        given(workOrderRepository.findById(500L)).willReturn(Optional.of(workOrder));
        given(userRepository.findByEmail("outsider@primatoos.test")).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(10L)).willReturn(Optional.of(outsider));

        assertThatThrownBy(() -> dailyReportService.create("outsider@primatoos.test",
                validCreateRequest(500L, null)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(dailyReportRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenWorkOrderDoesNotExist() {
        given(workOrderRepository.findById(500L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> dailyReportService.create("worker@primatoos.test", validCreateRequest(500L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowBusinessRuleException_whenWorkOrderIsNotReportable() {
        User user = aWorkerUser(10L, "worker@primatoos.test");
        Worker worker = aWorker(1L, user);
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.DRAFT, Set.of(worker));

        given(workOrderRepository.findById(500L)).willReturn(Optional.of(workOrder));
        given(userRepository.findByEmail("worker@primatoos.test")).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(10L)).willReturn(Optional.of(worker));

        assertThatThrownBy(() -> dailyReportService.create("worker@primatoos.test", validCreateRequest(500L, null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(dailyReportRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleException_whenItemIsPartiallyExecutedWithoutRequiredFields() {
        User user = aWorkerUser(10L, "worker@primatoos.test");
        Worker worker = aWorker(1L, user);
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of(worker));

        given(workOrderRepository.findById(500L)).willReturn(Optional.of(workOrder));
        given(userRepository.findByEmail("worker@primatoos.test")).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(10L)).willReturn(Optional.of(worker));

        DailyReportItemRequest incompleteItem = new DailyReportItemRequest("Levantar parede",
                DailyReportItemStatus.PARTIALLY_EXECUTED, null, null, null);

        assertThatThrownBy(() -> dailyReportService.create("worker@primatoos.test",
                validCreateRequest(500L, List.of(incompleteItem))))
                .isInstanceOf(BusinessRuleException.class);

        verify(dailyReportRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleException_whenItemIsNotExecutedWithoutRequiredFields() {
        User user = aWorkerUser(10L, "worker@primatoos.test");
        Worker worker = aWorker(1L, user);
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of(worker));

        given(workOrderRepository.findById(500L)).willReturn(Optional.of(workOrder));
        given(userRepository.findByEmail("worker@primatoos.test")).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(10L)).willReturn(Optional.of(worker));

        DailyReportItemRequest incompleteItem = new DailyReportItemRequest("Instalar janelas",
                DailyReportItemStatus.NOT_EXECUTED, "Chuva", null, LocalDate.of(2026, 8, 5));

        assertThatThrownBy(() -> dailyReportService.create("worker@primatoos.test",
                validCreateRequest(500L, List.of(incompleteItem))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldCreateDailyReport_whenItemIsPartiallyExecutedWithAllRequiredFields() {
        User user = aWorkerUser(10L, "worker@primatoos.test");
        Worker worker = aWorker(1L, user);
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of(worker));

        given(workOrderRepository.findById(500L)).willReturn(Optional.of(workOrder));
        given(userRepository.findByEmail("worker@primatoos.test")).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(10L)).willReturn(Optional.of(worker));
        given(dailyReportRepository.save(any(DailyReport.class))).willAnswer(invocation -> invocation.getArgument(0));

        DailyReportItemRequest completeItem = new DailyReportItemRequest("Levantar parede",
                DailyReportItemStatus.PARTIALLY_EXECUTED, "Falta de material", "Faltou cimento",
                LocalDate.of(2026, 8, 5));

        DailyReportResponse response = dailyReportService.create("worker@primatoos.test",
                validCreateRequest(500L, List.of(completeItem)));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).status()).isEqualTo(DailyReportItemStatus.PARTIALLY_EXECUTED);
    }

    @Test
    void shouldThrowBusinessRuleException_whenTeamPresentIncludesUnassignedWorker() {
        User user = aWorkerUser(10L, "worker@primatoos.test");
        Worker worker = aWorker(1L, user);
        Worker outsider = aWorker(99L, aWorkerUser(20L, "outsider@primatoos.test"));
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of(worker));

        given(workOrderRepository.findById(500L)).willReturn(Optional.of(workOrder));
        given(userRepository.findByEmail("worker@primatoos.test")).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(10L)).willReturn(Optional.of(worker));
        given(workerRepository.findAllById(Set.of(99L))).willReturn(List.of(outsider));

        DailyReportCreateRequest request = new DailyReportCreateRequest(500L, LocalDate.of(2026, 8, 1),
                Set.of(99L), null, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> dailyReportService.create("worker@primatoos.test", request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldThrowBusinessRuleException_whenEditingFinalizedReport() {
        User user = aWorkerUser(10L, "worker@primatoos.test");
        Worker worker = aWorker(1L, user);
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of(worker));
        DailyReport dailyReport = DailyReport.builder()
                .id(900L).workOrder(workOrder).date(LocalDate.of(2026, 8, 1)).filledByWorker(worker)
                .status(DailyReportStatus.FINALIZED).build();

        given(dailyReportRepository.findById(900L)).willReturn(Optional.of(dailyReport));
        given(userRepository.findByEmail("worker@primatoos.test")).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(10L)).willReturn(Optional.of(worker));

        assertThatThrownBy(() -> dailyReportService.update(900L, "worker@primatoos.test", validUpdateRequest(null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(dailyReportRepository, never()).save(any());
    }

    @Test
    void shouldAllowEdit_afterReopen() {
        User user = aWorkerUser(10L, "worker@primatoos.test");
        Worker worker = aWorker(1L, user);
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of(worker));
        DailyReport dailyReport = DailyReport.builder()
                .id(900L).workOrder(workOrder).date(LocalDate.of(2026, 8, 1)).filledByWorker(worker)
                .status(DailyReportStatus.FINALIZED).build();

        given(dailyReportRepository.findById(900L)).willReturn(Optional.of(dailyReport));
        given(dailyReportRepository.save(any(DailyReport.class))).willAnswer(invocation -> invocation.getArgument(0));

        DailyReportResponse reopened = dailyReportService.reopen(900L);
        assertThat(reopened.status()).isEqualTo(DailyReportStatus.DRAFT);

        given(userRepository.findByEmail("worker@primatoos.test")).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(10L)).willReturn(Optional.of(worker));

        DailyReportResponse updated = dailyReportService.update(900L, "worker@primatoos.test",
                validUpdateRequest(null));

        assertThat(updated.date()).isEqualTo(LocalDate.of(2026, 8, 2));
    }

    @Test
    void shouldThrowBusinessRuleException_whenReopeningDraftReport() {
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of());
        DailyReport dailyReport = DailyReport.builder()
                .id(900L).workOrder(workOrder).date(LocalDate.of(2026, 8, 1))
                .status(DailyReportStatus.DRAFT).build();

        given(dailyReportRepository.findById(900L)).willReturn(Optional.of(dailyReport));

        assertThatThrownBy(() -> dailyReportService.reopen(900L))
                .isInstanceOf(BusinessRuleException.class);

        verify(dailyReportRepository, never()).save(any());
    }

    @Test
    void shouldAllowManagerToViewAnyDailyReport() {
        User manager = User.builder().id(30L).name("Gestor").email("gestor@primatoos.test").password("hash")
                .role(UserRole.MANAGER).build();
        Worker worker = aWorker(1L, aWorkerUser(10L, "worker@primatoos.test"));
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of(worker));
        DailyReport dailyReport = DailyReport.builder()
                .id(900L).workOrder(workOrder).date(LocalDate.of(2026, 8, 1)).filledByWorker(worker)
                .status(DailyReportStatus.DRAFT).build();

        given(dailyReportRepository.findById(900L)).willReturn(Optional.of(dailyReport));
        given(userRepository.findByEmail("gestor@primatoos.test")).willReturn(Optional.of(manager));

        DailyReportResponse response = dailyReportService.findById(900L, "gestor@primatoos.test");

        assertThat(response.id()).isEqualTo(900L);
    }

    @Test
    void shouldThrowForbiddenOperationException_whenWorkerViewsReportForUnassignedWorkOrder() {
        User outsider = aWorkerUser(40L, "outsider@primatoos.test");
        Worker outsiderWorker = aWorker(2L, outsider);
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of());
        DailyReport dailyReport = DailyReport.builder()
                .id(900L).workOrder(workOrder).date(LocalDate.of(2026, 8, 1))
                .status(DailyReportStatus.DRAFT).build();

        given(dailyReportRepository.findById(900L)).willReturn(Optional.of(dailyReport));
        given(userRepository.findByEmail("outsider@primatoos.test")).willReturn(Optional.of(outsider));
        given(workerRepository.findByUserId(40L)).willReturn(Optional.of(outsiderWorker));

        assertThatThrownBy(() -> dailyReportService.findById(900L, "outsider@primatoos.test"))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void shouldReturnDailyReportsForWorkOrder_whenListingAsAssignedWorker() {
        User user = aWorkerUser(10L, "worker@primatoos.test");
        Worker worker = aWorker(1L, user);
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS, Set.of(worker));
        DailyReport dailyReport = DailyReport.builder()
                .id(900L).workOrder(workOrder).date(LocalDate.of(2026, 8, 1)).filledByWorker(worker)
                .status(DailyReportStatus.DRAFT).build();

        given(workOrderRepository.findById(500L)).willReturn(Optional.of(workOrder));
        given(userRepository.findByEmail("worker@primatoos.test")).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(10L)).willReturn(Optional.of(worker));

        Pageable pageable = PageRequest.of(0, 10);
        given(dailyReportRepository.findByWorkOrderId(500L, pageable)).willReturn(new PageImpl<>(List.of(dailyReport)));

        Page<DailyReportResponse> page = dailyReportService.findByWorkOrder(500L, "worker@primatoos.test", pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
