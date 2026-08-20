package com.primatoos.backend.dto.workorder;

import java.time.LocalDateTime;

public record WorkOrderPhotoResponse(Long id, String url, LocalDateTime createdAt) {
}
