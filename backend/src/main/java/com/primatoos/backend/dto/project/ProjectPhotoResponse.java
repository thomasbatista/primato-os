package com.primatoos.backend.dto.project;

import java.time.LocalDateTime;

public record ProjectPhotoResponse(Long id, String url, LocalDateTime createdAt) {
}
