package com.primatoos.backend.service;

import com.primatoos.backend.dto.materialrequest.DeliveryItemRequest;
import com.primatoos.backend.dto.materialrequest.MaterialRequestCreateRequest;
import com.primatoos.backend.dto.materialrequest.MaterialRequestFromWorkOrderRequest;
import com.primatoos.backend.dto.materialrequest.MaterialRequestItemRequest;
import com.primatoos.backend.dto.materialrequest.MaterialRequestResponse;
import com.primatoos.backend.dto.materialrequest.MaterialRequestUpdateRequest;
import com.primatoos.backend.dto.materialrequest.RegisterDeliveryRequest;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.mapper.MaterialRequestMapper;
import com.primatoos.backend.mapper.ProjectMapper;
import com.primatoos.backend.mapper.UserMapper;
import com.primatoos.backend.mapper.WorkOrderMapper;
import com.primatoos.backend.mapper.WorkerMapper;
import com.primatoos.backend.model.MaterialRequest;
import com.primatoos.backend.model.MaterialRequestItem;
import com.primatoos.backend.model.MaterialRequestPriority;
import com.primatoos.backend.model.MaterialRequestStatus;
import com.primatoos.backend.model.MaterialRequestUnit;
import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.repository.MaterialRequestRepository;
import com.primatoos.backend.repository.ProjectRepository;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MaterialRequestServiceTest {

    @Mock
    private MaterialRequestRepository materialRequestRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private UserRepository userRepository;

    private MaterialRequestService materialRequestService;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapper();
        ProjectMapper projectMapper = new ProjectMapper(userMapper);
        WorkOrderMapper workOrderMapper = new WorkOrderMapper(userMapper, new WorkerMapper(userMapper), projectMapper);
        materialRequestService = new MaterialRequestService(materialRequestRepository, projectRepository,
                workOrderRepository, userRepository,
                new MaterialRequestMapper(userMapper, projectMapper, workOrderMapper));
    }

    private Project aProject() {
        return Project.builder().id(1L).name("Obra A").client("Cliente A").build();
    }

    private User aManager() {
        return User.builder()
                .id(10L).name("Gestor").email("gestor@primatoos.test").password("hash").role(UserRole.MANAGER)
                .build();
    }

    private MaterialRequestItemRequest anItemRequest(BigDecimal quantity) {
        return new MaterialRequestItemRequest("Cimento", "CP-II 50kg", quantity, MaterialRequestUnit.BAG,
                "Votoran", null, null);
    }

    private MaterialRequestCreateRequest validCreateRequest(Long workOrderId, List<MaterialRequestItemRequest> items) {
        return new MaterialRequestCreateRequest(1L, workOrderId, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 10), 10L, MaterialRequestPriority.MEDIUM, "Falta material", null, "Almoxarifado",
                items);
    }

    private MaterialRequestUpdateRequest validUpdateRequest(List<MaterialRequestItemRequest> items) {
        return new MaterialRequestUpdateRequest(1L, null, LocalDate.of(2026, 8, 2), null, 10L,
                MaterialRequestPriority.HIGH, null, null, null, items);
    }

    private MaterialRequest aMaterialRequest(MaterialRequestStatus status, MaterialRequestItem... items) {
        MaterialRequest request = MaterialRequest.builder()
                .id(900L)
                .requestNumber(1L)
                .project(aProject())
                .requestDate(LocalDate.of(2026, 8, 1))
                .requester(aManager())
                .priority(MaterialRequestPriority.MEDIUM)
                .status(status)
                .build();

        for (MaterialRequestItem item : items) {
            item.setMaterialRequest(request);
            request.getItems().add(item);
        }

        return request;
    }

    private MaterialRequestItem anItem(Long id, BigDecimal quantity, BigDecimal delivered) {
        return MaterialRequestItem.builder()
                .id(id).name("Cimento").quantity(quantity).unit(MaterialRequestUnit.BAG)
                .quantityDelivered(delivered).build();
    }

    @Test
    void shouldCreateMaterialRequest_whenDataIsValid() {
        given(projectRepository.findById(1L)).willReturn(Optional.of(aProject()));
        given(userRepository.findById(10L)).willReturn(Optional.of(aManager()));
        given(materialRequestRepository.nextRequestNumber()).willReturn(1L);
        given(materialRequestRepository.save(any(MaterialRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MaterialRequestResponse response = materialRequestService.create(
                validCreateRequest(null, List.of(anItemRequest(BigDecimal.TEN))));

        assertThat(response.status()).isEqualTo(MaterialRequestStatus.DRAFT);
        assertThat(response.requestNumber()).isEqualTo(1L);
        assertThat(response.workOrder()).isNull();
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenProjectDoesNotExist() {
        given(projectRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> materialRequestService.create(
                validCreateRequest(null, List.of(anItemRequest(BigDecimal.TEN)))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(materialRequestRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleException_whenRequesterIsNotManager() {
        User worker = User.builder()
                .id(10L).name("Joao").email("joao@primatoos.test").password("hash").role(UserRole.WORKER).build();

        given(projectRepository.findById(1L)).willReturn(Optional.of(aProject()));
        given(userRepository.findById(10L)).willReturn(Optional.of(worker));

        assertThatThrownBy(() -> materialRequestService.create(
                validCreateRequest(null, List.of(anItemRequest(BigDecimal.TEN)))))
                .isInstanceOf(BusinessRuleException.class);

        verify(materialRequestRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenWorkOrderDoesNotExist() {
        given(projectRepository.findById(1L)).willReturn(Optional.of(aProject()));
        given(workOrderRepository.findById(500L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> materialRequestService.create(
                validCreateRequest(500L, List.of(anItemRequest(BigDecimal.TEN)))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldSubmitRequest_whenStatusIsDraft() {
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.DRAFT);
        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));
        given(materialRequestRepository.save(any(MaterialRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MaterialRequestResponse response = materialRequestService.submit(900L);

        assertThat(response.status()).isEqualTo(MaterialRequestStatus.REQUESTED);
    }

    @Test
    void shouldThrowBusinessRuleException_whenApprovingDraftRequest() {
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.DRAFT);
        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> materialRequestService.approve(900L))
                .isInstanceOf(BusinessRuleException.class);

        verify(materialRequestRepository, never()).save(any());
    }

    @Test
    void shouldApproveRequest_whenStatusIsRequested() {
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.REQUESTED);
        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));
        given(materialRequestRepository.save(any(MaterialRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MaterialRequestResponse response = materialRequestService.approve(900L);

        assertThat(response.status()).isEqualTo(MaterialRequestStatus.APPROVED);
    }

    @Test
    void shouldMarkPurchased_whenStatusIsApproved() {
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.APPROVED);
        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));
        given(materialRequestRepository.save(any(MaterialRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MaterialRequestResponse response = materialRequestService.markPurchased(900L);

        assertThat(response.status()).isEqualTo(MaterialRequestStatus.PURCHASED);
    }

    @Test
    void shouldCancelRequest_whenStatusIsDraft() {
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.DRAFT);
        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));
        given(materialRequestRepository.save(any(MaterialRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MaterialRequestResponse response = materialRequestService.cancel(900L);

        assertThat(response.status()).isEqualTo(MaterialRequestStatus.CANCELLED);
    }

    @Test
    void shouldThrowBusinessRuleException_whenCancellingDeliveredRequest() {
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.DELIVERED);
        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> materialRequestService.cancel(900L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldMarkPartiallyDelivered_whenSomeItemsNotFullyDelivered() {
        MaterialRequestItem item1 = anItem(1L, BigDecimal.TEN, BigDecimal.ZERO);
        MaterialRequestItem item2 = anItem(2L, BigDecimal.TEN, BigDecimal.ZERO);
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.PURCHASED, item1, item2);

        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));
        given(materialRequestRepository.save(any(MaterialRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        RegisterDeliveryRequest delivery = new RegisterDeliveryRequest(
                List.of(new DeliveryItemRequest(1L, BigDecimal.TEN)));

        MaterialRequestResponse response = materialRequestService.registerDelivery(900L, delivery);

        assertThat(response.status()).isEqualTo(MaterialRequestStatus.PARTIALLY_DELIVERED);
    }

    @Test
    void shouldMarkDelivered_whenAllItemsFullyDelivered() {
        MaterialRequestItem item1 = anItem(1L, BigDecimal.TEN, BigDecimal.ZERO);
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.PURCHASED, item1);

        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));
        given(materialRequestRepository.save(any(MaterialRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        RegisterDeliveryRequest delivery = new RegisterDeliveryRequest(
                List.of(new DeliveryItemRequest(1L, BigDecimal.TEN)));

        MaterialRequestResponse response = materialRequestService.registerDelivery(900L, delivery);

        assertThat(response.status()).isEqualTo(MaterialRequestStatus.DELIVERED);
        assertThat(response.items().get(0).quantityDelivered()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void shouldAccumulateDeliveredQuantity_acrossMultipleDeliveryRegistrations() {
        MaterialRequestItem item1 = anItem(1L, BigDecimal.TEN, new BigDecimal("4"));
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.PARTIALLY_DELIVERED, item1);

        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));
        given(materialRequestRepository.save(any(MaterialRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        RegisterDeliveryRequest delivery = new RegisterDeliveryRequest(
                List.of(new DeliveryItemRequest(1L, new BigDecimal("6"))));

        MaterialRequestResponse response = materialRequestService.registerDelivery(900L, delivery);

        assertThat(response.status()).isEqualTo(MaterialRequestStatus.DELIVERED);
        assertThat(response.items().get(0).quantityDelivered()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void shouldThrowBusinessRuleException_whenDeliveredQuantityExceedsRequested() {
        MaterialRequestItem item1 = anItem(1L, BigDecimal.TEN, BigDecimal.ZERO);
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.PURCHASED, item1);

        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));

        RegisterDeliveryRequest delivery = new RegisterDeliveryRequest(
                List.of(new DeliveryItemRequest(1L, new BigDecimal("11"))));

        assertThatThrownBy(() -> materialRequestService.registerDelivery(900L, delivery))
                .isInstanceOf(BusinessRuleException.class);

        verify(materialRequestRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleException_whenRegisteringDeliveryOnDraftRequest() {
        MaterialRequestItem item1 = anItem(1L, BigDecimal.TEN, BigDecimal.ZERO);
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.DRAFT, item1);

        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));

        RegisterDeliveryRequest delivery = new RegisterDeliveryRequest(
                List.of(new DeliveryItemRequest(1L, BigDecimal.ONE)));

        assertThatThrownBy(() -> materialRequestService.registerDelivery(900L, delivery))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenDeliveryReferencesUnknownItem() {
        MaterialRequestItem item1 = anItem(1L, BigDecimal.TEN, BigDecimal.ZERO);
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.PURCHASED, item1);

        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));

        RegisterDeliveryRequest delivery = new RegisterDeliveryRequest(
                List.of(new DeliveryItemRequest(999L, BigDecimal.ONE)));

        assertThatThrownBy(() -> materialRequestService.registerDelivery(900L, delivery))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowBusinessRuleException_whenEditingCancelledRequest() {
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.CANCELLED);
        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> materialRequestService.update(900L,
                validUpdateRequest(List.of(anItemRequest(BigDecimal.ONE)))))
                .isInstanceOf(BusinessRuleException.class);

        verify(materialRequestRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleException_whenEditingDeliveredRequest() {
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.DELIVERED);
        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> materialRequestService.update(900L,
                validUpdateRequest(List.of(anItemRequest(BigDecimal.ONE)))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldUpdateMaterialRequest_whenStatusIsDraft() {
        MaterialRequest request = aMaterialRequest(MaterialRequestStatus.DRAFT);
        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(request));
        given(projectRepository.findById(1L)).willReturn(Optional.of(aProject()));
        given(userRepository.findById(10L)).willReturn(Optional.of(aManager()));
        given(materialRequestRepository.save(any(MaterialRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MaterialRequestResponse response = materialRequestService.update(900L,
                validUpdateRequest(List.of(anItemRequest(new BigDecimal("5")))));

        assertThat(response.priority()).isEqualTo(MaterialRequestPriority.HIGH);
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void shouldDuplicateMaterialRequest_resettingStatusAndNeededByDate() {
        MaterialRequestItem item1 = anItem(1L, BigDecimal.TEN, BigDecimal.TEN);
        MaterialRequest original = aMaterialRequest(MaterialRequestStatus.DELIVERED, item1);
        original.setNeededByDate(LocalDate.of(2026, 8, 5));

        given(materialRequestRepository.findById(900L)).willReturn(Optional.of(original));
        given(materialRequestRepository.nextRequestNumber()).willReturn(2L);
        given(materialRequestRepository.save(any(MaterialRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MaterialRequestResponse response = materialRequestService.duplicate(900L);

        assertThat(response.status()).isEqualTo(MaterialRequestStatus.DRAFT);
        assertThat(response.requestNumber()).isEqualTo(2L);
        assertThat(response.neededByDate()).isNull();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantityDelivered()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCreateFromWorkOrder_derivingProjectAndRequester() {
        Project project = aProject();
        User manager = aManager();
        WorkOrder workOrder = WorkOrder.builder()
                .id(500L).orderNumber(1L).project(project).stage("Fundação").description("Concretar").build();

        given(workOrderRepository.findById(500L)).willReturn(Optional.of(workOrder));
        given(userRepository.findByEmail("gestor@primatoos.test")).willReturn(Optional.of(manager));
        given(materialRequestRepository.nextRequestNumber()).willReturn(1L);
        given(materialRequestRepository.save(any(MaterialRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MaterialRequestFromWorkOrderRequest request = new MaterialRequestFromWorkOrderRequest(
                LocalDate.of(2026, 8, 10), MaterialRequestPriority.URGENT, "Urgente", null, null,
                List.of(anItemRequest(BigDecimal.TEN)));

        MaterialRequestResponse response = materialRequestService.createFromWorkOrder(500L,
                "gestor@primatoos.test", request);

        assertThat(response.project().id()).isEqualTo(1L);
        assertThat(response.workOrder().id()).isEqualTo(500L);
        assertThat(response.requester().id()).isEqualTo(10L);
        assertThat(response.priority()).isEqualTo(MaterialRequestPriority.URGENT);
    }

    @Test
    void shouldThrowBusinessRuleException_whenCreatingFromWorkOrderAsNonManager() {
        User worker = User.builder()
                .id(20L).name("Joao").email("joao@primatoos.test").password("hash").role(UserRole.WORKER).build();
        WorkOrder workOrder = WorkOrder.builder()
                .id(500L).orderNumber(1L).project(aProject()).stage("Fundação").description("Concretar").build();

        given(workOrderRepository.findById(500L)).willReturn(Optional.of(workOrder));
        given(userRepository.findByEmail("joao@primatoos.test")).willReturn(Optional.of(worker));

        MaterialRequestFromWorkOrderRequest request = new MaterialRequestFromWorkOrderRequest(
                null, MaterialRequestPriority.LOW, null, null, null, List.of(anItemRequest(BigDecimal.ONE)));

        assertThatThrownBy(() -> materialRequestService.createFromWorkOrder(500L, "joao@primatoos.test", request))
                .isInstanceOf(BusinessRuleException.class);

        verify(materialRequestRepository, never()).save(any());
    }
}
