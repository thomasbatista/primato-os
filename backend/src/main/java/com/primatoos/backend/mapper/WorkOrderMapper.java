package com.primatoos.backend.mapper;

import com.primatoos.backend.dto.project.ProjectSummaryResponse;
import com.primatoos.backend.dto.workorder.WorkOrderResponse;
import com.primatoos.backend.dto.worker.WorkerSummaryResponse;
import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.model.Worker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class WorkOrderMapper {

    private final UserMapper userMapper;

    public WorkOrderResponse toResponse(WorkOrder workOrder) {
        return new WorkOrderResponse(
                workOrder.getId(),
                workOrder.getOrderNumber(),
                toProjectSummary(workOrder.getProject()),
                workOrder.getDate(),
                userMapper.toSummary(workOrder.getResponsibleUser()),
                workOrder.getStage(),
                workOrder.getLocation(),
                workOrder.getDescription(),
                workOrder.getDailyGoal(),
                workOrder.getPlannedStartTime(),
                workOrder.getPlannedEndTime(),
                workOrder.getMaterialsNeeded(),
                workOrder.getTools(),
                workOrder.getSafetyGuidelines(),
                workOrder.getQualityCriteria(),
                workOrder.getNotes(),
                workOrder.getStatus(),
                workOrder.getAssignedWorkers().stream()
                        .map(this::toWorkerSummary)
                        .sorted(Comparator.comparing(WorkerSummaryResponse::name))
                        .toList(),
                workOrder.getCreatedAt()
        );
    }

    private ProjectSummaryResponse toProjectSummary(Project project) {
        return new ProjectSummaryResponse(project.getId(), project.getName(), project.getClient());
    }

    private WorkerSummaryResponse toWorkerSummary(Worker worker) {
        return new WorkerSummaryResponse(worker.getId(), worker.getName(), worker.getFunction());
    }
}
