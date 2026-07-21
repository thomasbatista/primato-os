package com.primatoos.backend.dto.dailyreport;

import com.primatoos.backend.dto.worker.WorkerSummaryResponse;
import com.primatoos.backend.dto.workorder.WorkOrderSummaryResponse;
import com.primatoos.backend.model.DailyReportStatus;

import java.time.LocalDate;

public record DailyReportSummaryResponse(
        Long id,
        WorkOrderSummaryResponse workOrder,
        LocalDate date,
        WorkerSummaryResponse filledByWorker,
        DailyReportStatus status
) {
}
