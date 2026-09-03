package com.orderhub.notification_service.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String customerId,
        String customerEmail,
        BigDecimal totalAmount
) {
}
