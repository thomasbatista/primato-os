package com.primatoos.backend.dto.user;

import com.primatoos.backend.model.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        LocalDateTime createdAt
) {
}
