package com.primatoos.backend.service;

import com.primatoos.backend.dto.dailyreport.DailyReportCreateRequest;
import com.primatoos.backend.dto.dailyreport.DailyReportItemRequest;
import com.primatoos.backend.dto.dailyreport.DailyReportResponse;
import com.primatoos.backend.dto.dailyreport.DailyReportUpdateRequest;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.ForbiddenOperationException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.mapper.DailyReportMapper;
import com.primatoos.backend.model.DailyReport;
import com.primatoos.backend.model.DailyReportItem;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyReportService {

    private static final Set<WorkOrderStatus> REPORTABLE_WORK_ORDER_STATUSES =
            Set.of(WorkOrderStatus.RELEASED, WorkOrderStatus.IN_PROGRESS);

    private final DailyReportRepository dailyReportRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final DailyReportMapper dailyReportMapper;

    public DailyReportResponse create(String email, DailyReportCreateRequest request) {
        WorkOrder workOrder = findWorkOrderOrThrow(request.workOrderId());
        Worker filledByWorker = resolveAssignedWorkerOrThrow(email, workOrder);
        ensureWorkOrderIsReportable(workOrder);
        Set<Worker> teamPresent = resolveTeamPresentOrThrow(request.teamPresentWorkerIds(), workOrder);

        DailyReport dailyReport = DailyReport.builder()
                .workOrder(workOrder)
                .date(request.date())
                .filledByWorker(filledByWorker)
                .teamPresent(teamPresent)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .weatherCondition(request.weatherCondition())
                .extraServicesExecuted(request.extraServicesExecuted())
                .problemsFound(request.problemsFound())
                .pendingIssuesGenerated(request.pendingIssuesGenerated())
                .materialsUsed(request.materialsUsed())
                .materialsMissing(request.materialsMissing())
                .forecastForNextDay(request.forecastForNextDay())
                .notes(request.notes())
                .status(DailyReportStatus.DRAFT)
                .build();

        applyItems(dailyReport, request.items());

        return dailyReportMapper.toResponse(dailyReportRepository.save(dailyReport));
    }

    public Page<DailyReportResponse> findByWorkOrder(Long workOrderId, String email, Pageable pageable) {
        WorkOrder workOrder = findWorkOrderOrThrow(workOrderId);
        enforceViewPermission(email, workOrder);

        return dailyReportRepository.findByWorkOrderId(workOrderId, pageable).map(dailyReportMapper::toResponse);
    }

    public DailyReportResponse findById(Long id, String email) {
        DailyReport dailyReport = findDailyReportOrThrow(id);
        enforceViewPermission(email, dailyReport.getWorkOrder());

        return dailyReportMapper.toResponse(dailyReport);
    }

    public DailyReportResponse update(Long id, String email, DailyReportUpdateRequest request) {
        DailyReport dailyReport = findDailyReportOrThrow(id);
        resolveAssignedWorkerOrThrow(email, dailyReport.getWorkOrder());
        ensureDraft(dailyReport);

        Set<Worker> teamPresent = resolveTeamPresentOrThrow(request.teamPresentWorkerIds(), dailyReport.getWorkOrder());

        dailyReport.setDate(request.date());
        dailyReport.setTeamPresent(teamPresent);
        dailyReport.setStartTime(request.startTime());
        dailyReport.setEndTime(request.endTime());
        dailyReport.setWeatherCondition(request.weatherCondition());
        dailyReport.setExtraServicesExecuted(request.extraServicesExecuted());
        dailyReport.setProblemsFound(request.problemsFound());
        dailyReport.setPendingIssuesGenerated(request.pendingIssuesGenerated());
        dailyReport.setMaterialsUsed(request.materialsUsed());
        dailyReport.setMaterialsMissing(request.materialsMissing());
        dailyReport.setForecastForNextDay(request.forecastForNextDay());
        dailyReport.setNotes(request.notes());

        dailyReport.getItems().clear();
        applyItems(dailyReport, request.items());

        return dailyReportMapper.toResponse(dailyReportRepository.save(dailyReport));
    }

    public DailyReportResponse finalizeReport(Long id, String email) {
        DailyReport dailyReport = findDailyReportOrThrow(id);
        resolveAssignedWorkerOrThrow(email, dailyReport.getWorkOrder());
        ensureDraft(dailyReport);

        dailyReport.setStatus(DailyReportStatus.FINALIZED);
        return dailyReportMapper.toResponse(dailyReportRepository.save(dailyReport));
    }

    public DailyReportResponse reopen(Long id) {
        DailyReport dailyReport = findDailyReportOrThrow(id);

        if (dailyReport.getStatus() != DailyReportStatus.FINALIZED) {
            throw new BusinessRuleException("Só é possível reabrir um Checklist Diário finalizado");
        }

        dailyReport.setStatus(DailyReportStatus.DRAFT);
        return dailyReportMapper.toResponse(dailyReportRepository.save(dailyReport));
    }

    private void applyItems(DailyReport dailyReport, List<DailyReportItemRequest> itemRequests) {
        List<DailyReportItem> items = new ArrayList<>();

        if (itemRequests != null) {
            for (DailyReportItemRequest itemRequest : itemRequests) {
                validateItemConditionalFields(itemRequest);

                items.add(DailyReportItem.builder()
                        .dailyReport(dailyReport)
                        .activityDescription(itemRequest.activityDescription())
                        .status(itemRequest.status())
                        .reason(itemRequest.reason())
                        .observation(itemRequest.observation())
                        .newExpectedDate(itemRequest.newExpectedDate())
                        .build());
            }
        }

        dailyReport.getItems().addAll(items);
    }

    private void validateItemConditionalFields(DailyReportItemRequest itemRequest) {
        boolean requiresDetails = itemRequest.status() == DailyReportItemStatus.PARTIALLY_EXECUTED
                || itemRequest.status() == DailyReportItemStatus.NOT_EXECUTED;

        if (!requiresDetails) {
            return;
        }

        if (isBlank(itemRequest.reason()) || isBlank(itemRequest.observation()) || itemRequest.newExpectedDate() == null) {
            throw new BusinessRuleException("Motivo, observação e nova data prevista são obrigatórios quando o "
                    + "item não foi executado ou foi executado parcialmente");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void ensureDraft(DailyReport dailyReport) {
        if (dailyReport.getStatus() != DailyReportStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Só é possível editar um Checklist Diário em rascunho. Reabra-o antes de editar.");
        }
    }

    private void ensureWorkOrderIsReportable(WorkOrder workOrder) {
        if (!REPORTABLE_WORK_ORDER_STATUSES.contains(workOrder.getStatus())) {
            throw new BusinessRuleException(
                    "Só é possível preencher o Checklist Diário de uma OS liberada ou em andamento");
        }
    }

    private void enforceViewPermission(String email, WorkOrder workOrder) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (user.getRole() == UserRole.MANAGER) {
            return;
        }

        resolveAssignedWorkerOrThrow(email, workOrder);
    }

    private Worker resolveAssignedWorkerOrThrow(String email, WorkOrder workOrder) {
        Worker worker = resolveWorkerOrThrow(email);

        boolean isAssigned = workOrder.getAssignedWorkers().stream()
                .anyMatch(assigned -> assigned.getId().equals(worker.getId()));

        if (!isAssigned) {
            throw new ForbiddenOperationException("Você não está atribuído a esta ordem de serviço");
        }

        return worker;
    }

    private Worker resolveWorkerOrThrow(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return workerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Você não está vinculado a um perfil de colaborador"));
    }

    private Set<Worker> resolveTeamPresentOrThrow(Set<Long> workerIds, WorkOrder workOrder) {
        if (workerIds == null || workerIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Worker> found = workerRepository.findAllById(workerIds);

        if (found.size() != workerIds.size()) {
            throw new ResourceNotFoundException("Um ou mais colaboradores informados não foram encontrados");
        }

        Set<Long> assignedIds = workOrder.getAssignedWorkers().stream()
                .map(Worker::getId)
                .collect(Collectors.toSet());

        boolean allAssigned = found.stream().allMatch(worker -> assignedIds.contains(worker.getId()));

        if (!allAssigned) {
            throw new BusinessRuleException("A equipe presente deve ser um subconjunto dos colaboradores atribuídos à OS");
        }

        return new HashSet<>(found);
    }

    private DailyReport findDailyReportOrThrow(Long id) {
        return dailyReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist Diário não encontrado"));
    }

    private WorkOrder findWorkOrderOrThrow(Long workOrderId) {
        return workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada"));
    }
}
