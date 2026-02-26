package com.adriano.orderhub.dto.order;

import com.adriano.orderhub.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
}
