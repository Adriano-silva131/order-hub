package com.adriano.catalogservice.dto.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockAdjustmentRequest(
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        Integer quantity
) {
}
