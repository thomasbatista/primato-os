package com.primatoos.backend.dto.dailyreport;

import com.primatoos.backend.model.DailyReportItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DailyReportItemRequest(

        @NotBlank(message = "Descrição da atividade é obrigatória")
        String activityDescription,

        @NotNull(message = "Status do item é obrigatório")
        DailyReportItemStatus status,

        String reason,
        String observation,
        LocalDate newExpectedDate
) {
}
