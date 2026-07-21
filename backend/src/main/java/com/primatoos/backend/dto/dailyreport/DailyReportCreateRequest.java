package com.primatoos.backend.dto.dailyreport;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public record DailyReportCreateRequest(

        @NotNull(message = "Ordem de serviço é obrigatória")
        Long workOrderId,

        @NotNull(message = "Data é obrigatória")
        LocalDate date,

        Set<Long> teamPresentWorkerIds,
        LocalTime startTime,
        LocalTime endTime,
        String weatherCondition,
        String extraServicesExecuted,
        String problemsFound,
        String pendingIssuesGenerated,
        String materialsUsed,
        String materialsMissing,
        String forecastForNextDay,
        String notes,

        @Valid
        List<DailyReportItemRequest> items
) {
}
