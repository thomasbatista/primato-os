package com.primatoos.backend.dto.materialrequest;

import com.primatoos.backend.model.MaterialRequestUnit;

import java.math.BigDecimal;

public record MaterialRequestItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal quantity,
        MaterialRequestUnit unit,
        String brand,
        String photoReference,
        String notes,
        BigDecimal quantityDelivered
) {
}
