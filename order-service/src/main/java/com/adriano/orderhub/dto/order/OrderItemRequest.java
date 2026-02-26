package com.adriano.orderhub.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotBlank(message = "Product ID is required")
        String productId,

        @NotEmpty(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        Integer quantity
) {
}
