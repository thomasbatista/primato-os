package com.primatoos.backend.dto.project;

import com.primatoos.backend.dto.common.UserSummaryResponse;
import com.primatoos.backend.model.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String client,
        String address,
        UserSummaryResponse responsibleUser,
        LocalDate startDate,
        LocalDate expectedDeadline,
        String currentStage,
        ProjectStatus status,
        String notes,
        LocalDateTime createdAt
) {
}
