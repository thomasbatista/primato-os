package com.primatoos.backend.service;

import com.primatoos.backend.dto.workorder.WorkOrderCreateRequest;
import com.primatoos.backend.dto.workorder.WorkOrderResponse;
import com.primatoos.backend.dto.workorder.WorkOrderUpdateRequest;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.mapper.UserMapper;
import com.primatoos.backend.mapper.WorkOrderMapper;
import com.primatoos.backend.mapper.WorkerMapper;
import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.model.WorkOrderStatus;
import com.primatoos.backend.model.Worker;
import com.primatoos.backend.repository.ProjectRepository;
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
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkerRepository workerRepository;

    private WorkOrderService workOrderService;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapper();
        workOrderService = new WorkOrderService(workOrderRepository, projectRepository, userRepository,
                workerRepository, new WorkOrderMapper(userMapper, new WorkerMapper(userMapper)));
    }

    private Project aProject() {
        return Project.builder().id(1L).name("Obra A").client("Cliente A").build();
    }

    private User aManager() {
        return User.builder()
                .id(10L).name("Gestor").email("gestor@primatoos.test").password("hash").role(UserRole.MANAGER)
                .build();
    }

    private WorkOrderCreateRequest validCreateRequest(Set<Long> workerIds) {
        return new WorkOrderCreateRequest(1L, LocalDate.of(2026, 8, 1), 10L, "Fundação", "Bloco A",
                "Concretar fundação", "20m2", null, null, null, null, null, null, null, workerIds);
    }

    private WorkOrderUpdateRequest validUpdateRequest(Set<Long> workerIds) {
        return new WorkOrderUpdateRequest(1L, LocalDate.of(2026, 8, 2), 10L, "Alvenaria", "Bloco B",
                "Levantar paredes", "15m2", null, null, null, null, null, null, null, workerIds);
    }

    private WorkOrder aWorkOrder(WorkOrderStatus status) {
        return WorkOrder.builder()
                .id(100L)
                .orderNumber(1L)
                .project(aProject())
                .date(LocalDate.of(2026, 8, 1))
                .responsibleUser(aManager())
                .stage("Fundação")
                .description("Concretar fundação")
                .status(status)
                .build();
    }

    @Test
    void shouldCreateWorkOrder_whenDataIsValid() {
        given(projectRepository.findById(1L)).willReturn(Optional.of(aProject()));
        given(userRepository.findById(10L)).willReturn(Optional.of(aManager()));
        given(workOrderRepository.nextOrderNumber()).willReturn(1L);
        given(workOrderRepository.save(any(WorkOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkOrderResponse response = workOrderService.create(validCreateRequest(null));

        assertThat(response.status()).isEqualTo(WorkOrderStatus.DRAFT);
        assertThat(response.orderNumber()).isEqualTo(1L);
        assertThat(response.project().id()).isEqualTo(1L);
        assertThat(response.assignedWorkers()).isEmpty();
    }

    @Test
    void shouldThrowResourceNotFoundException_whenProjectDoesNotExist() {
        given(projectRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workOrderService.create(validCreateRequest(null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenResponsibleUserDoesNotExist() {
        given(projectRepository.findById(1L)).willReturn(Optional.of(aProject()));
        given(userRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workOrderService.create(validCreateRequest(null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowBusinessRuleException_whenResponsibleUserIsNotManager() {
        User worker = User.builder()
                .id(10L).name("Joao").email("joao@primatoos.test").password("hash").role(UserRole.WORKER).build();

        given(projectRepository.findById(1L)).willReturn(Optional.of(aProject()));
        given(userRepository.findById(10L)).willReturn(Optional.of(worker));

        assertThatThrownBy(() -> workOrderService.create(validCreateRequest(null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenAssignedWorkerDoesNotExist() {
        given(projectRepository.findById(1L)).willReturn(Optional.of(aProject()));
        given(userRepository.findById(10L)).willReturn(Optional.of(aManager()));
        given(workerRepository.findAllById(Set.of(99L))).willReturn(List.of());

        assertThatThrownBy(() -> workOrderService.create(validCreateRequest(Set.of(99L))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowBusinessRuleException_whenAssignedWorkerIsInactive() {
        Worker inactiveWorker = Worker.builder().id(5L).name("Carlos").active(false).build();

        given(projectRepository.findById(1L)).willReturn(Optional.of(aProject()));
        given(userRepository.findById(10L)).willReturn(Optional.of(aManager()));
        given(workerRepository.findAllById(Set.of(5L))).willReturn(List.of(inactiveWorker));

        assertThatThrownBy(() -> workOrderService.create(validCreateRequest(Set.of(5L))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldReleaseWorkOrder_whenStatusIsDraft() {
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.DRAFT);
        given(workOrderRepository.findById(100L)).willReturn(Optional.of(workOrder));
        given(workOrderRepository.save(any(WorkOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkOrderResponse response = workOrderService.release(100L);

        assertThat(response.status()).isEqualTo(WorkOrderStatus.RELEASED);
    }

    @Test
    void shouldThrowBusinessRuleException_whenCompletingDraftOrder() {
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.DRAFT);
        given(workOrderRepository.findById(100L)).willReturn(Optional.of(workOrder));

        assertThatThrownBy(() -> workOrderService.complete(100L))
                .isInstanceOf(BusinessRuleException.class);

        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void shouldStartWorkOrder_whenStatusIsReleased() {
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.RELEASED);
        given(workOrderRepository.findById(100L)).willReturn(Optional.of(workOrder));
        given(workOrderRepository.save(any(WorkOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkOrderResponse response = workOrderService.start(100L);

        assertThat(response.status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
    }

    @Test
    void shouldThrowBusinessRuleException_whenStartingDraftOrder() {
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.DRAFT);
        given(workOrderRepository.findById(100L)).willReturn(Optional.of(workOrder));

        assertThatThrownBy(() -> workOrderService.start(100L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldCompleteWorkOrder_whenStatusIsInProgress() {
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.IN_PROGRESS);
        given(workOrderRepository.findById(100L)).willReturn(Optional.of(workOrder));
        given(workOrderRepository.save(any(WorkOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkOrderResponse response = workOrderService.complete(100L);

        assertThat(response.status()).isEqualTo(WorkOrderStatus.COMPLETED);
    }

    @Test
    void shouldCancelWorkOrder_whenStatusIsDraft() {
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.DRAFT);
        given(workOrderRepository.findById(100L)).willReturn(Optional.of(workOrder));
        given(workOrderRepository.save(any(WorkOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkOrderResponse response = workOrderService.cancel(100L);

        assertThat(response.status()).isEqualTo(WorkOrderStatus.CANCELLED);
    }

    @Test
    void shouldThrowBusinessRuleException_whenCancellingCompletedOrder() {
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.COMPLETED);
        given(workOrderRepository.findById(100L)).willReturn(Optional.of(workOrder));

        assertThatThrownBy(() -> workOrderService.cancel(100L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldThrowBusinessRuleException_whenEditingCancelledOrder() {
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.CANCELLED);
        given(workOrderRepository.findById(100L)).willReturn(Optional.of(workOrder));

        assertThatThrownBy(() -> workOrderService.update(100L, validUpdateRequest(null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleException_whenEditingCompletedOrder() {
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.COMPLETED);
        given(workOrderRepository.findById(100L)).willReturn(Optional.of(workOrder));

        assertThatThrownBy(() -> workOrderService.update(100L, validUpdateRequest(null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldUpdateWorkOrder_whenStatusIsDraft() {
        WorkOrder workOrder = aWorkOrder(WorkOrderStatus.DRAFT);
        given(workOrderRepository.findById(100L)).willReturn(Optional.of(workOrder));
        given(projectRepository.findById(1L)).willReturn(Optional.of(aProject()));
        given(userRepository.findById(10L)).willReturn(Optional.of(aManager()));
        given(workOrderRepository.save(any(WorkOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkOrderResponse response = workOrderService.update(100L, validUpdateRequest(null));

        assertThat(response.stage()).isEqualTo("Alvenaria");
        assertThat(response.description()).isEqualTo("Levantar paredes");
    }

    @Test
    void shouldDuplicateWorkOrder_resettingStatusAndPlannedTimes() {
        WorkOrder original = aWorkOrder(WorkOrderStatus.COMPLETED);
        given(workOrderRepository.findById(100L)).willReturn(Optional.of(original));
        given(workOrderRepository.nextOrderNumber()).willReturn(2L);
        given(workOrderRepository.save(any(WorkOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkOrderResponse response = workOrderService.duplicate(100L);

        assertThat(response.status()).isEqualTo(WorkOrderStatus.DRAFT);
        assertThat(response.orderNumber()).isEqualTo(2L);
        assertThat(response.plannedStartTime()).isNull();
        assertThat(response.plannedEndTime()).isNull();
        assertThat(response.stage()).isEqualTo(original.getStage());
    }

    @Test
    void shouldReturnEmptyPage_whenUserHasNoLinkedWorkerProfile() {
        given(userRepository.findByEmail("no-worker@primatoos.test")).willReturn(Optional.of(
                User.builder().id(20L).name("Sem Perfil").email("no-worker@primatoos.test").password("hash")
                        .role(UserRole.WORKER).build()));
        given(workerRepository.findByUserId(20L)).willReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 10);
        Page<WorkOrderResponse> page = workOrderService.findMyWorkOrders("no-worker@primatoos.test", pageable);

        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void shouldReturnAssignedWorkOrders_whenWorkerHasAssignments() {
        User user = User.builder()
                .id(21L).name("Com Perfil").email("com-worker@primatoos.test").password("hash")
                .role(UserRole.WORKER).build();
        Worker worker = Worker.builder().id(7L).name("Com Perfil").user(user).build();
        WorkOrder assigned = aWorkOrder(WorkOrderStatus.RELEASED);

        given(userRepository.findByEmail("com-worker@primatoos.test")).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(21L)).willReturn(Optional.of(worker));

        Pageable pageable = PageRequest.of(0, 10);
        given(workOrderRepository.findByAssignedWorkers_Id(7L, pageable))
                .willReturn(new PageImpl<>(List.of(assigned)));

        Page<WorkOrderResponse> page = workOrderService.findMyWorkOrders("com-worker@primatoos.test", pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).id()).isEqualTo(100L);
    }
}
