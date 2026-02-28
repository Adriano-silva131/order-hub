package com.orderhub.payment_service.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
    UUID orderId,
    String customerId,
    BigDecimal totalAmount
) { }
