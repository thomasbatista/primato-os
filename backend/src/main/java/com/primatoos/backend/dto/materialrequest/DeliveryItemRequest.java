package com.primatoos.backend.dto.materialrequest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DeliveryItemRequest(

        @NotNull(message = "Item do pedido é obrigatório")
        Long materialRequestItemId,

        @NotNull(message = "Quantidade entregue é obrigatória")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantidade entregue deve ser maior que zero")
        BigDecimal quantityDelivered
) {
}
