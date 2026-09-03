package com.adriano.catalogservice.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Map;

public record ProductRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Description is required")
        String description,
        @NotNull(message = "Price is required")
        @Positive(message = "Price is bigger than zero")
        BigDecimal price,
        boolean active,
        Map<String, Object> attributes,
        @PositiveOrZero(message = "Stock quantity cannot be negative")
        int stockQuantity
) {
}
