package com.primatoos.backend.dto.workorder;

import com.primatoos.backend.dto.common.UserSummaryResponse;
import com.primatoos.backend.dto.project.ProjectSummaryResponse;
import com.primatoos.backend.dto.worker.WorkerSummaryResponse;
import com.primatoos.backend.model.WorkOrderStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record WorkOrderResponse(
        Long id,
        Long orderNumber,
        ProjectSummaryResponse project,
        LocalDate date,
        UserSummaryResponse responsibleUser,
        String stage,
        String location,
        String description,
        String dailyGoal,
        LocalTime plannedStartTime,
        LocalTime plannedEndTime,
        String materialsNeeded,
        String tools,
        String safetyGuidelines,
        String qualityCriteria,
        String notes,
        WorkOrderStatus status,
        List<WorkerSummaryResponse> assignedWorkers,
        LocalDateTime createdAt
) {
}
