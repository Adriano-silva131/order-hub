package com.adriano.orderhub.dto.order;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "Items are required")
        List<OrderItemRequest> items
) {
}
