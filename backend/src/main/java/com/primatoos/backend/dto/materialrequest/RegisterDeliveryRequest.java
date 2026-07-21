package com.primatoos.backend.dto.materialrequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RegisterDeliveryRequest(

        @NotEmpty(message = "Informe ao menos um item entregue")
        @Valid
        List<DeliveryItemRequest> items
) {
}
