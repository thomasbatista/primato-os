package com.primatoos.backend.dto.dailyreport;

import com.primatoos.backend.model.DailyReportItemStatus;

import java.time.LocalDate;

public record DailyReportItemResponse(
        Long id,
        String activityDescription,
        DailyReportItemStatus status,
        String reason,
        String observation,
        LocalDate newExpectedDate
) {
}
