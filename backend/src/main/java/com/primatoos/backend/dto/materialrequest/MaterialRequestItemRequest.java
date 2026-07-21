package com.primatoos.backend.dto.materialrequest;

import com.primatoos.backend.model.MaterialRequestUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MaterialRequestItemRequest(

        @NotBlank(message = "Nome do material é obrigatório")
        String name,

        String description,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantidade deve ser maior que zero")
        BigDecimal quantity,

        @NotNull(message = "Unidade é obrigatória")
        MaterialRequestUnit unit,

        String brand,
        String photoReference,
        String notes
) {
}
