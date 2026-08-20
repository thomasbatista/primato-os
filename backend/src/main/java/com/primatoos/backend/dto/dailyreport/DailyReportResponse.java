package com.primatoos.backend.dto.dailyreport;

import com.primatoos.backend.dto.common.UserSummaryResponse;
import com.primatoos.backend.dto.worker.WorkerSummaryResponse;
import com.primatoos.backend.dto.workorder.WorkOrderSummaryResponse;
import com.primatoos.backend.model.DailyReportStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record DailyReportResponse(
        Long id,
        WorkOrderSummaryResponse workOrder,
        LocalDate date,
        // Exactly one of these is non-null: the worker who filled it, or the manager who did.
        WorkerSummaryResponse filledByWorker,
        UserSummaryResponse filledByUser,
        List<WorkerSummaryResponse> teamPresent,
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
        DailyReportStatus status,
        List<DailyReportItemResponse> items,
        List<DailyReportPhotoResponse> photos,
        LocalDateTime createdAt
) {
}
