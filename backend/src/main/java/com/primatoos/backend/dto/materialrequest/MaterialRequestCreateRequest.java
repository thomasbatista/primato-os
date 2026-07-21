package com.primatoos.backend.dto.materialrequest;

import com.primatoos.backend.model.MaterialRequestPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record MaterialRequestCreateRequest(

        @NotNull(message = "Obra é obrigatória")
        Long projectId,

        Long workOrderId,

        @NotNull(message = "Data do pedido é obrigatória")
        LocalDate requestDate,

        LocalDate neededByDate,

        @NotNull(message = "Solicitante é obrigatório")
        Long requesterId,

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
