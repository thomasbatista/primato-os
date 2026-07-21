package com.primatoos.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProjectCreateRequest(

        @NotBlank(message = "Nome da obra é obrigatório")
        String name,

        @NotBlank(message = "Cliente é obrigatório")
        String client,

        String address,

        @NotNull(message = "Responsável é obrigatório")
        Long responsibleUserId,

        LocalDate startDate,
        LocalDate expectedDeadline,
        String currentStage,
        String notes
) {
}
