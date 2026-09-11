package com.adriano.orderhub.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        Long orderNumber,
        String customerId,
        String customerEmail,
        BigDecimal totalAmount,
        Instant createdAt
) {
}
