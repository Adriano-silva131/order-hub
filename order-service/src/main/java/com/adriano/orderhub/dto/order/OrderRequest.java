package com.adriano.orderhub.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest(
        @NotBlank(message = "Customer is required")
        String customerId,

        @NotEmpty(message = "Items are required")
        List<OrderItemRequest> items
) {
}
