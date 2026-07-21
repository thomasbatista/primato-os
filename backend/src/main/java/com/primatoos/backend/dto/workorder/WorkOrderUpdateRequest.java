package com.primatoos.backend.dto.workorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record WorkOrderUpdateRequest(

        @NotNull(message = "Obra é obrigatória")
        Long projectId,

        @NotNull(message = "Data é obrigatória")
        LocalDate date,

        @NotNull(message = "Responsável é obrigatório")
        Long responsibleUserId,

        @NotBlank(message = "Etapa é obrigatória")
        String stage,

        String location,

        @NotBlank(message = "Descrição é obrigatória")
        String description,

        String dailyGoal,
        LocalTime plannedStartTime,
        LocalTime plannedEndTime,
        String materialsNeeded,
        String tools,
        String safetyGuidelines,
        String qualityCriteria,
        String notes,
        Set<Long> assignedWorkerIds
) {
}
