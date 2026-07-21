package com.primatoos.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkerUpdateRequest(

        @NotBlank(message = "Nome é obrigatório")
        String name,

        String function,
        String phone,
        Long userId
) {
}
