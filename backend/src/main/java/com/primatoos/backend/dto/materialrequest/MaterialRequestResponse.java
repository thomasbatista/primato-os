package com.primatoos.backend.dto.materialrequest;

import com.primatoos.backend.dto.common.UserSummaryResponse;
import com.primatoos.backend.dto.project.ProjectSummaryResponse;
import com.primatoos.backend.dto.workorder.WorkOrderSummaryResponse;
import com.primatoos.backend.model.MaterialRequestPriority;
import com.primatoos.backend.model.MaterialRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MaterialRequestResponse(
        Long id,
        Long requestNumber,
        ProjectSummaryResponse project,
        WorkOrderSummaryResponse workOrder,
        LocalDate requestDate,
        LocalDate neededByDate,
        UserSummaryResponse requester,
        MaterialRequestPriority priority,
        String justification,
        String notes,
        String deliveryLocation,
        MaterialRequestStatus status,
        List<MaterialRequestItemResponse> items,
        LocalDateTime createdAt
) {
}
