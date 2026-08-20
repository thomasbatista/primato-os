package com.primatoos.backend.service;

import com.primatoos.backend.dto.dailyreport.DailyReportCreateRequest;
import com.primatoos.backend.dto.dailyreport.DailyReportItemRequest;
import com.primatoos.backend.dto.dailyreport.DailyReportPhotoResponse;
import com.primatoos.backend.dto.dailyreport.DailyReportResponse;
import com.primatoos.backend.dto.dailyreport.DailyReportUpdateRequest;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.ForbiddenOperationException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.mapper.DailyReportMapper;
import com.primatoos.backend.model.DailyReport;
import com.primatoos.backend.model.DailyReportItem;
import com.primatoos.backend.model.DailyReportItemStatus;
import com.primatoos.backend.model.DailyReportPhoto;
import com.primatoos.backend.model.DailyReportStatus;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.model.WorkOrderStatus;
import com.primatoos.backend.model.Worker;
import com.primatoos.backend.repository.DailyReportPhotoRepository;
import com.primatoos.backend.repository.DailyReportRepository;
import com.primatoos.backend.repository.WorkOrderRepository;
import com.primatoos.backend.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyReportService {

    private static final Set<WorkOrderStatus> REPORTABLE_WORK_ORDER_STATUSES =
            Set.of(WorkOrderStatus.RELEASED, WorkOrderStatus.IN_PROGRESS);

    private final DailyReportRepository dailyReportRepository;
    private final DailyReportPhotoRepository dailyReportPhotoRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkerRepository workerRepository;
    private final DailyReportMapper dailyReportMapper;
    private final StorageService storageService;
    private final PhotoUploadSupport photoUploadSupport;
    private final CallerResolver callerResolver;

    public DailyReportResponse create(String email, DailyReportCreateRequest request) {
        WorkOrder workOrder = findWorkOrderOrThrow(request.workOrderId());
        User caller = callerResolver.findUserOrThrow(email);
        boolean filledByManager = callerResolver.isManager(caller);

        // Authorization runs before any business rule, so an unassigned worker can't learn
        // anything about an OS they have no claim on. A manager fills reports for any OS;
        // a worker only for one they are assigned to.
        Worker filledByWorker = filledByManager ? null : resolveAssignedWorkerOrThrow(caller, workOrder);

        ensureWorkOrderIsReportable(workOrder);
        Set<Worker> teamPresent = resolveTeamPresentOrThrow(request.teamPresentWorkerIds(), workOrder);

        DailyReport dailyReport = DailyReport.builder()
                .workOrder(workOrder)
                .date(request.date())
                .filledByWorker(filledByWorker)
                .filledByUser(filledByManager ? caller : null)
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

    public Page<DailyReportResponse> findMyReports(String email, Long workOrderId, Pageable pageable) {
        Worker worker = callerResolver.resolveWorkerOrThrow(email);

        return dailyReportRepository.findByFilledByWorker(worker.getId(), workOrderId, pageable)
                .map(dailyReportMapper::toResponse);
    }

    public DailyReportResponse findById(Long id, String email) {
        DailyReport dailyReport = findDailyReportOrThrow(id);
        enforceViewPermission(email, dailyReport.getWorkOrder());

        return dailyReportMapper.toResponse(dailyReport);
    }

    public DailyReportResponse update(Long id, String email, DailyReportUpdateRequest request) {
        DailyReport dailyReport = findDailyReportOrThrow(id);
        enforceFillPermission(email, dailyReport.getWorkOrder());
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
        enforceFillPermission(email, dailyReport.getWorkOrder());
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

    public DailyReportPhotoResponse uploadPhoto(Long dailyReportId, String email, MultipartFile file) {
        DailyReport dailyReport = findDailyReportOrThrow(dailyReportId);
        enforceFillPermission(email, dailyReport.getWorkOrder());
        ensureDraft(dailyReport);

        String extension = photoUploadSupport.validateAndGetExtension(file);
        String key = "daily-reports/" + dailyReportId + "/" + UUID.randomUUID() + "." + extension;
        String url = storageService.upload(key, photoUploadSupport.readBytes(file), file.getContentType());

        DailyReportPhoto photo = DailyReportPhoto.builder()
                .dailyReport(dailyReport)
                .url(url)
                .build();

        return dailyReportMapper.toPhotoResponse(dailyReportPhotoRepository.save(photo));
    }

    public void deletePhoto(Long dailyReportId, Long photoId, String email) {
        DailyReport dailyReport = findDailyReportOrThrow(dailyReportId);
        enforceFillPermission(email, dailyReport.getWorkOrder());
        ensureDraft(dailyReport);

        DailyReportPhoto photo = dailyReportPhotoRepository.findById(photoId)
                .filter(candidate -> candidate.getDailyReport().getId().equals(dailyReportId))
                .orElseThrow(() -> new ResourceNotFoundException("Foto não encontrada"));

        storageService.delete(photo.getUrl());
        dailyReportPhotoRepository.delete(photo);
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
        enforceFillPermission(email, workOrder);
    }

    // Filling a report (create/edit/finalize, including its photos) is open to any manager —
    // they own every OS — but a worker still has to be assigned to the OS in question.
    private void enforceFillPermission(String email, WorkOrder workOrder) {
        User caller = callerResolver.findUserOrThrow(email);

        if (callerResolver.isManager(caller)) {
            return;
        }

        resolveAssignedWorkerOrThrow(caller, workOrder);
    }

    private Worker resolveAssignedWorkerOrThrow(User caller, WorkOrder workOrder) {
        Worker worker = callerResolver.resolveWorkerOrThrow(caller);

        boolean isAssigned = workOrder.getAssignedWorkers().stream()
                .anyMatch(assigned -> assigned.getId().equals(worker.getId()));

        if (!isAssigned) {
            throw new ForbiddenOperationException("Você não está atribuído a esta ordem de serviço");
        }

        return worker;
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
