package com.primatoos.backend.mapper;

import com.primatoos.backend.dto.workorder.WorkOrderPhotoResponse;
import com.primatoos.backend.dto.workorder.WorkOrderResponse;
import com.primatoos.backend.dto.workorder.WorkOrderSummaryResponse;
import com.primatoos.backend.dto.worker.WorkerSummaryResponse;
import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.model.WorkOrderPhoto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class WorkOrderMapper {

    private final UserMapper userMapper;
    private final WorkerMapper workerMapper;
    private final ProjectMapper projectMapper;

    public WorkOrderResponse toResponse(WorkOrder workOrder) {
        return new WorkOrderResponse(
                workOrder.getId(),
                workOrder.getOrderNumber(),
                projectMapper.toSummary(workOrder.getProject()),
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
                        .map(workerMapper::toSummary)
                        .sorted(Comparator.comparing(WorkerSummaryResponse::name))
                        .toList(),
                workOrder.getCreatedAt()
        );
    }

    public WorkOrderSummaryResponse toSummary(WorkOrder workOrder) {
        return new WorkOrderSummaryResponse(workOrder.getId(), workOrder.getOrderNumber(), workOrder.getStage());
    }

    public WorkOrderPhotoResponse toPhotoResponse(WorkOrderPhoto photo) {
        return new WorkOrderPhotoResponse(photo.getId(), photo.getUrl(), photo.getCreatedAt());
    }
}
