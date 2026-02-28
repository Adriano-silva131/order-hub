package com.orderhub.payment_service.event;

import java.util.UUID;

public record PaymentProcessedEvent(
        UUID orderId,
        String status
) {}
