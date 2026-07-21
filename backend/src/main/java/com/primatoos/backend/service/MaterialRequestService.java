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
import com.primatoos.backend.model.MaterialRequest;
import com.primatoos.backend.model.MaterialRequestItem;
import com.primatoos.backend.model.MaterialRequestStatus;
import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.repository.MaterialRequestRepository;
import com.primatoos.backend.repository.ProjectRepository;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialRequestService {

    private static final Map<MaterialRequestStatus, Set<MaterialRequestStatus>> ALLOWED_TRANSITIONS = Map.of(
            MaterialRequestStatus.DRAFT, Set.of(MaterialRequestStatus.REQUESTED, MaterialRequestStatus.CANCELLED),
            MaterialRequestStatus.REQUESTED, Set.of(MaterialRequestStatus.APPROVED, MaterialRequestStatus.CANCELLED),
            MaterialRequestStatus.APPROVED, Set.of(MaterialRequestStatus.PURCHASED, MaterialRequestStatus.CANCELLED),
            MaterialRequestStatus.PURCHASED, Set.of(MaterialRequestStatus.CANCELLED),
            MaterialRequestStatus.PARTIALLY_DELIVERED, Set.of(MaterialRequestStatus.CANCELLED),
            MaterialRequestStatus.DELIVERED, Set.of(),
            MaterialRequestStatus.CANCELLED, Set.of()
    );

    private final MaterialRequestRepository materialRequestRepository;
    private final ProjectRepository projectRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final MaterialRequestMapper materialRequestMapper;

    public MaterialRequestResponse create(MaterialRequestCreateRequest request) {
        Project project = findProjectOrThrow(request.projectId());
        WorkOrder workOrder = resolveWorkOrderOrNull(request.workOrderId());
        User requester = findManagerOrThrow(request.requesterId());

        MaterialRequest materialRequest = MaterialRequest.builder()
                .requestNumber(materialRequestRepository.nextRequestNumber())
                .project(project)
                .workOrder(workOrder)
                .requestDate(request.requestDate())
                .neededByDate(request.neededByDate())
                .requester(requester)
                .priority(request.priority())
                .justification(request.justification())
                .notes(request.notes())
                .deliveryLocation(request.deliveryLocation())
                .status(MaterialRequestStatus.DRAFT)
                .build();

        applyItems(materialRequest, request.items());

        return materialRequestMapper.toResponse(materialRequestRepository.save(materialRequest));
    }

    public MaterialRequestResponse createFromWorkOrder(Long workOrderId, String requesterEmail,
                                                         MaterialRequestFromWorkOrderRequest request) {
        WorkOrder workOrder = findWorkOrderOrThrow(workOrderId);
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (requester.getRole() != UserRole.MANAGER) {
            throw new BusinessRuleException("Apenas um gestor pode gerar um pedido de materiais");
        }

        MaterialRequest materialRequest = MaterialRequest.builder()
                .requestNumber(materialRequestRepository.nextRequestNumber())
                .project(workOrder.getProject())
                .workOrder(workOrder)
                .requestDate(LocalDate.now())
                .neededByDate(request.neededByDate())
                .requester(requester)
                .priority(request.priority())
                .justification(request.justification())
                .notes(request.notes())
                .deliveryLocation(request.deliveryLocation())
                .status(MaterialRequestStatus.DRAFT)
                .build();

        applyItems(materialRequest, request.items());

        return materialRequestMapper.toResponse(materialRequestRepository.save(materialRequest));
    }

    public Page<MaterialRequestResponse> findAll(Long projectId, Long workOrderId, MaterialRequestStatus status,
                                                  Pageable pageable) {
        return materialRequestRepository.search(projectId, workOrderId, status, pageable)
                .map(materialRequestMapper::toResponse);
    }

    public MaterialRequestResponse findById(Long id) {
        return materialRequestMapper.toResponse(findMaterialRequestOrThrow(id));
    }

    public MaterialRequestResponse update(Long id, MaterialRequestUpdateRequest request) {
        MaterialRequest materialRequest = findMaterialRequestOrThrow(id);
        ensureEditable(materialRequest);

        Project project = findProjectOrThrow(request.projectId());
        WorkOrder workOrder = resolveWorkOrderOrNull(request.workOrderId());
        User requester = findManagerOrThrow(request.requesterId());

        materialRequest.setProject(project);
        materialRequest.setWorkOrder(workOrder);
        materialRequest.setRequestDate(request.requestDate());
        materialRequest.setNeededByDate(request.neededByDate());
        materialRequest.setRequester(requester);
        materialRequest.setPriority(request.priority());
        materialRequest.setJustification(request.justification());
        materialRequest.setNotes(request.notes());
        materialRequest.setDeliveryLocation(request.deliveryLocation());

        materialRequest.getItems().clear();
        applyItems(materialRequest, request.items());

        return materialRequestMapper.toResponse(materialRequestRepository.save(materialRequest));
    }

    public MaterialRequestResponse submit(Long id) {
        return transition(id, MaterialRequestStatus.REQUESTED);
    }

    public MaterialRequestResponse approve(Long id) {
        return transition(id, MaterialRequestStatus.APPROVED);
    }

    public MaterialRequestResponse markPurchased(Long id) {
        return transition(id, MaterialRequestStatus.PURCHASED);
    }

    public MaterialRequestResponse cancel(Long id) {
        return transition(id, MaterialRequestStatus.CANCELLED);
    }

    public MaterialRequestResponse registerDelivery(Long id, RegisterDeliveryRequest request) {
        MaterialRequest materialRequest = findMaterialRequestOrThrow(id);
        ensureDeliverable(materialRequest);

        Map<Long, MaterialRequestItem> itemsById = materialRequest.getItems().stream()
                .collect(Collectors.toMap(MaterialRequestItem::getId, item -> item));

        for (DeliveryItemRequest deliveryItem : request.items()) {
            MaterialRequestItem item = itemsById.get(deliveryItem.materialRequestItemId());

            if (item == null) {
                throw new ResourceNotFoundException(
                        "Item do pedido não encontrado: " + deliveryItem.materialRequestItemId());
            }

            BigDecimal newTotal = item.getQuantityDelivered().add(deliveryItem.quantityDelivered());

            if (newTotal.compareTo(item.getQuantity()) > 0) {
                throw new BusinessRuleException(
                        "Quantidade entregue não pode exceder a quantidade solicitada para o item: " + item.getName());
            }

            item.setQuantityDelivered(newTotal);
        }

        recomputeStatusAfterDelivery(materialRequest);

        return materialRequestMapper.toResponse(materialRequestRepository.save(materialRequest));
    }

    public MaterialRequestResponse duplicate(Long id) {
        MaterialRequest original = findMaterialRequestOrThrow(id);

        MaterialRequest copy = MaterialRequest.builder()
                .requestNumber(materialRequestRepository.nextRequestNumber())
                .project(original.getProject())
                .workOrder(original.getWorkOrder())
                .requestDate(LocalDate.now())
                .neededByDate(null)
                .requester(original.getRequester())
                .priority(original.getPriority())
                .justification(original.getJustification())
                .notes(original.getNotes())
                .deliveryLocation(original.getDeliveryLocation())
                .status(MaterialRequestStatus.DRAFT)
                .build();

        List<MaterialRequestItem> copiedItems = original.getItems().stream()
                .map(item -> MaterialRequestItem.builder()
                        .materialRequest(copy)
                        .name(item.getName())
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .unit(item.getUnit())
                        .brand(item.getBrand())
                        .photoReference(item.getPhotoReference())
                        .notes(item.getNotes())
                        .quantityDelivered(BigDecimal.ZERO)
                        .build())
                .toList();

        copy.getItems().addAll(copiedItems);

        return materialRequestMapper.toResponse(materialRequestRepository.save(copy));
    }

    private void applyItems(MaterialRequest materialRequest, List<MaterialRequestItemRequest> itemRequests) {
        List<MaterialRequestItem> items = new ArrayList<>();

        for (MaterialRequestItemRequest itemRequest : itemRequests) {
            items.add(MaterialRequestItem.builder()
                    .materialRequest(materialRequest)
                    .name(itemRequest.name())
                    .description(itemRequest.description())
                    .quantity(itemRequest.quantity())
                    .unit(itemRequest.unit())
                    .brand(itemRequest.brand())
                    .photoReference(itemRequest.photoReference())
                    .notes(itemRequest.notes())
                    .quantityDelivered(BigDecimal.ZERO)
                    .build());
        }

        materialRequest.getItems().addAll(items);
    }

    private MaterialRequestResponse transition(Long id, MaterialRequestStatus target) {
        MaterialRequest materialRequest = findMaterialRequestOrThrow(id);
        MaterialRequestStatus current = materialRequest.getStatus();

        if (!ALLOWED_TRANSITIONS.get(current).contains(target)) {
            throw new BusinessRuleException(
                    "Não é possível mudar o status do pedido de materiais de " + current + " para " + target);
        }

        materialRequest.setStatus(target);
        return materialRequestMapper.toResponse(materialRequestRepository.save(materialRequest));
    }

    private void ensureEditable(MaterialRequest materialRequest) {
        if (materialRequest.getStatus() == MaterialRequestStatus.CANCELLED
                || materialRequest.getStatus() == MaterialRequestStatus.DELIVERED) {
            throw new BusinessRuleException("Não é possível editar um pedido de materiais cancelado ou já entregue");
        }
    }

    private void ensureDeliverable(MaterialRequest materialRequest) {
        MaterialRequestStatus status = materialRequest.getStatus();

        if (status != MaterialRequestStatus.PURCHASED && status != MaterialRequestStatus.PARTIALLY_DELIVERED) {
            throw new BusinessRuleException(
                    "Só é possível registrar entregas de um pedido comprado ou parcialmente entregue");
        }
    }

    private void recomputeStatusAfterDelivery(MaterialRequest materialRequest) {
        boolean allFullyDelivered = materialRequest.getItems().stream()
                .allMatch(item -> item.getQuantityDelivered().compareTo(item.getQuantity()) >= 0);

        materialRequest.setStatus(
                allFullyDelivered ? MaterialRequestStatus.DELIVERED : MaterialRequestStatus.PARTIALLY_DELIVERED);
    }

    private MaterialRequest findMaterialRequestOrThrow(Long id) {
        return materialRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de materiais não encontrado"));
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Obra não encontrada"));
    }

    private WorkOrder findWorkOrderOrThrow(Long workOrderId) {
        return workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada"));
    }

    private WorkOrder resolveWorkOrderOrNull(Long workOrderId) {
        if (workOrderId == null) {
            return null;
        }

        return findWorkOrderOrThrow(workOrderId);
    }

    private User findManagerOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário solicitante não encontrado"));

        if (user.getRole() != UserRole.MANAGER) {
            throw new BusinessRuleException("O solicitante deve ser um usuário com papel de gestor");
        }

        return user;
    }
}
