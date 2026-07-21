package com.primatoos.backend.dto.materialrequest;

import com.primatoos.backend.model.MaterialRequestPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record MaterialRequestFromWorkOrderRequest(

        LocalDate neededByDate,

        @NotNull(message = "Prioridade é obrigatória")
        MaterialRequestPriority priority,

        String justification,
        String notes,
        String deliveryLocation,

        @NotEmpty(message = "O pedido deve conter ao menos um item")
        @Valid
        List<MaterialRequestItemRequest> items
) {
}
