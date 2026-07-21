package com.primatoos.backend.dto.worker;

import com.primatoos.backend.dto.common.UserSummaryResponse;

import java.time.LocalDateTime;

public record WorkerResponse(
        Long id,
        String name,
        String function,
        String phone,
        boolean active,
        UserSummaryResponse user,
        LocalDateTime createdAt
) {
}
