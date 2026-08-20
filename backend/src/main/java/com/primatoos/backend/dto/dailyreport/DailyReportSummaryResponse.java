package com.primatoos.backend.dto.dailyreport;

import com.primatoos.backend.dto.common.UserSummaryResponse;
import com.primatoos.backend.dto.worker.WorkerSummaryResponse;
import com.primatoos.backend.dto.workorder.WorkOrderSummaryResponse;
import com.primatoos.backend.model.DailyReportStatus;

import java.time.LocalDate;

public record DailyReportSummaryResponse(
        Long id,
        WorkOrderSummaryResponse workOrder,
        LocalDate date,
        // Exactly one of these is non-null: the worker who filled it, or the manager who did.
        WorkerSummaryResponse filledByWorker,
        UserSummaryResponse filledByUser,
        DailyReportStatus status
) {
}
