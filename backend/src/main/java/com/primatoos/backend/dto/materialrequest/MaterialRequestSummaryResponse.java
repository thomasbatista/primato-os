package com.primatoos.backend.dto.materialrequest;

import com.primatoos.backend.dto.project.ProjectSummaryResponse;
import com.primatoos.backend.model.MaterialRequestPriority;
import com.primatoos.backend.model.MaterialRequestStatus;

public record MaterialRequestSummaryResponse(
        Long id,
        Long requestNumber,
        ProjectSummaryResponse project,
        MaterialRequestPriority priority,
        MaterialRequestStatus status
) {
}
