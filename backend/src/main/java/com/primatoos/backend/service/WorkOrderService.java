package com.primatoos.backend.service;

import com.primatoos.backend.dto.workorder.WorkOrderCreateRequest;
import com.primatoos.backend.dto.workorder.WorkOrderResponse;
import com.primatoos.backend.dto.workorder.WorkOrderUpdateRequest;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.mapper.WorkOrderMapper;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            WorkOrderStatus.DRAFT, Set.of(WorkOrderStatus.RELEASED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.RELEASED, Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.IN_PROGRESS, Set.of(WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.COMPLETED, Set.of(),
            WorkOrderStatus.CANCELLED, Set.of()
    );

    private final WorkOrderRepository workOrderRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final WorkOrderMapper workOrderMapper;

    public WorkOrderResponse create(WorkOrderCreateRequest request) {
        Project project = findProjectOrThrow(request.projectId());
        User responsibleUser = findManagerOrThrow(request.responsibleUserId());
        Set<Worker> assignedWorkers = resolveWorkersOrThrow(request.assignedWorkerIds());

        WorkOrder workOrder = WorkOrder.builder()
                .orderNumber(workOrderRepository.nextOrderNumber())
                .project(project)
                .date(request.date())
                .responsibleUser(responsibleUser)
                .stage(request.stage())
                .location(request.location())
                .description(request.description())
                .dailyGoal(request.dailyGoal())
                .plannedStartTime(request.plannedStartTime())
                .plannedEndTime(request.plannedEndTime())
                .materialsNeeded(request.materialsNeeded())
                .tools(request.tools())
                .safetyGuidelines(request.safetyGuidelines())
                .qualityCriteria(request.qualityCriteria())
                .notes(request.notes())
                .status(WorkOrderStatus.DRAFT)
                .assignedWorkers(assignedWorkers)
                .build();

        return workOrderMapper.toResponse(workOrderRepository.save(workOrder));
    }

    public Page<WorkOrderResponse> findAll(Long projectId, WorkOrderStatus status, Pageable pageable) {
        return workOrderRepository.search(projectId, status, pageable).map(workOrderMapper::toResponse);
    }

    public WorkOrderResponse findById(Long id) {
        return workOrderMapper.toResponse(findWorkOrderOrThrow(id));
    }

    public WorkOrderResponse update(Long id, WorkOrderUpdateRequest request) {
        WorkOrder workOrder = findWorkOrderOrThrow(id);
        ensureEditable(workOrder);

        Project project = findProjectOrThrow(request.projectId());
        User responsibleUser = findManagerOrThrow(request.responsibleUserId());
        Set<Worker> assignedWorkers = resolveWorkersOrThrow(request.assignedWorkerIds());

        workOrder.setProject(project);
        workOrder.setDate(request.date());
        workOrder.setResponsibleUser(responsibleUser);
        workOrder.setStage(request.stage());
        workOrder.setLocation(request.location());
        workOrder.setDescription(request.description());
        workOrder.setDailyGoal(request.dailyGoal());
        workOrder.setPlannedStartTime(request.plannedStartTime());
        workOrder.setPlannedEndTime(request.plannedEndTime());
        workOrder.setMaterialsNeeded(request.materialsNeeded());
        workOrder.setTools(request.tools());
        workOrder.setSafetyGuidelines(request.safetyGuidelines());
        workOrder.setQualityCriteria(request.qualityCriteria());
        workOrder.setNotes(request.notes());
        workOrder.setAssignedWorkers(assignedWorkers);

        return workOrderMapper.toResponse(workOrderRepository.save(workOrder));
    }

    public WorkOrderResponse release(Long id) {
        return transition(id, WorkOrderStatus.RELEASED);
    }

    public WorkOrderResponse start(Long id) {
        return transition(id, WorkOrderStatus.IN_PROGRESS);
    }

    public WorkOrderResponse complete(Long id) {
        return transition(id, WorkOrderStatus.COMPLETED);
    }

    public WorkOrderResponse cancel(Long id) {
        return transition(id, WorkOrderStatus.CANCELLED);
    }

    public WorkOrderResponse duplicate(Long id) {
        WorkOrder original = findWorkOrderOrThrow(id);

        WorkOrder copy = WorkOrder.builder()
                .orderNumber(workOrderRepository.nextOrderNumber())
                .project(original.getProject())
                .date(original.getDate())
                .responsibleUser(original.getResponsibleUser())
                .stage(original.getStage())
                .location(original.getLocation())
                .description(original.getDescription())
                .dailyGoal(original.getDailyGoal())
                .plannedStartTime(null)
                .plannedEndTime(null)
                .materialsNeeded(original.getMaterialsNeeded())
                .tools(original.getTools())
                .safetyGuidelines(original.getSafetyGuidelines())
                .qualityCriteria(original.getQualityCriteria())
                .notes(original.getNotes())
                .status(WorkOrderStatus.DRAFT)
                .assignedWorkers(new HashSet<>(original.getAssignedWorkers()))
                .build();

        return workOrderMapper.toResponse(workOrderRepository.save(copy));
    }

    public Page<WorkOrderResponse> findMyWorkOrders(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return workerRepository.findByUserId(user.getId())
                .map(worker -> workOrderRepository.findByAssignedWorkers_Id(worker.getId(), pageable)
                        .map(workOrderMapper::toResponse))
                .orElse(Page.empty(pageable));
    }

    private WorkOrderResponse transition(Long id, WorkOrderStatus target) {
        WorkOrder workOrder = findWorkOrderOrThrow(id);
        WorkOrderStatus current = workOrder.getStatus();

        if (!ALLOWED_TRANSITIONS.get(current).contains(target)) {
            throw new BusinessRuleException(
                    "Não é possível mudar o status da OS de " + current + " para " + target);
        }

        workOrder.setStatus(target);
        return workOrderMapper.toResponse(workOrderRepository.save(workOrder));
    }

    private void ensureEditable(WorkOrder workOrder) {
        if (workOrder.getStatus() == WorkOrderStatus.CANCELLED || workOrder.getStatus() == WorkOrderStatus.COMPLETED) {
            throw new BusinessRuleException("Não é possível editar uma OS cancelada ou concluída");
        }
    }

    private WorkOrder findWorkOrderOrThrow(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada"));
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Obra não encontrada"));
    }

    private User findManagerOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));

        if (user.getRole() != UserRole.MANAGER) {
            throw new BusinessRuleException("O responsável pela OS deve ser um usuário com papel de gestor");
        }

        return user;
    }

    private Set<Worker> resolveWorkersOrThrow(Set<Long> workerIds) {
        if (workerIds == null || workerIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Worker> found = workerRepository.findAllById(workerIds);

        if (found.size() != workerIds.size()) {
            throw new ResourceNotFoundException("Um ou mais colaboradores informados não foram encontrados");
        }

        boolean hasInactiveWorker = found.stream().anyMatch(worker -> !worker.isActive());
        if (hasInactiveWorker) {
            throw new BusinessRuleException("Não é possível atribuir colaboradores inativos a uma OS");
        }

        return new HashSet<>(found);
    }
}
