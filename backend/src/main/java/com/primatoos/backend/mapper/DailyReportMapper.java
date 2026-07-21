package com.primatoos.backend.mapper;

import com.primatoos.backend.dto.dailyreport.DailyReportItemResponse;
import com.primatoos.backend.dto.dailyreport.DailyReportPhotoResponse;
import com.primatoos.backend.dto.dailyreport.DailyReportResponse;
import com.primatoos.backend.dto.worker.WorkerSummaryResponse;
import com.primatoos.backend.dto.workorder.WorkOrderSummaryResponse;
import com.primatoos.backend.model.DailyReport;
import com.primatoos.backend.model.DailyReportItem;
import com.primatoos.backend.model.DailyReportPhoto;
import com.primatoos.backend.model.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class DailyReportMapper {

    private final WorkerMapper workerMapper;

    public DailyReportResponse toResponse(DailyReport dailyReport) {
        return new DailyReportResponse(
                dailyReport.getId(),
                toWorkOrderSummary(dailyReport.getWorkOrder()),
                dailyReport.getDate(),
                workerMapper.toSummary(dailyReport.getFilledByWorker()),
                dailyReport.getTeamPresent().stream()
                        .map(workerMapper::toSummary)
                        .sorted(Comparator.comparing(WorkerSummaryResponse::name))
                        .toList(),
                dailyReport.getStartTime(),
                dailyReport.getEndTime(),
                dailyReport.getWeatherCondition(),
                dailyReport.getExtraServicesExecuted(),
                dailyReport.getProblemsFound(),
                dailyReport.getPendingIssuesGenerated(),
                dailyReport.getMaterialsUsed(),
                dailyReport.getMaterialsMissing(),
                dailyReport.getForecastForNextDay(),
                dailyReport.getNotes(),
                dailyReport.getStatus(),
                dailyReport.getItems().stream().map(this::toItemResponse).toList(),
                dailyReport.getPhotos().stream().map(this::toPhotoResponse).toList(),
                dailyReport.getCreatedAt()
        );
    }

    private WorkOrderSummaryResponse toWorkOrderSummary(WorkOrder workOrder) {
        return new WorkOrderSummaryResponse(workOrder.getId(), workOrder.getOrderNumber(), workOrder.getStage());
    }

    private DailyReportItemResponse toItemResponse(DailyReportItem item) {
        return new DailyReportItemResponse(
                item.getId(),
                item.getActivityDescription(),
                item.getStatus(),
                item.getReason(),
                item.getObservation(),
                item.getNewExpectedDate()
        );
    }

    private DailyReportPhotoResponse toPhotoResponse(DailyReportPhoto photo) {
        return new DailyReportPhotoResponse(photo.getId(), photo.getUrl(), photo.getCreatedAt());
    }
}
