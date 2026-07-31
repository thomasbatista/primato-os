package com.primatoos.backend.dto.auth;

import com.primatoos.backend.model.UserRole;

public record AuthMeResponse(String name, String email, UserRole role) {
}
