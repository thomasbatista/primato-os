package com.primatoos.backend.dto.dailyreport;

import java.time.LocalDateTime;

public record DailyReportPhotoResponse(Long id, String url, LocalDateTime createdAt) {
}
