package com.adriano.orderhub.event;

import java.util.UUID;

public record PaymentProcessedEvent(
        UUID orderId,
        String status
) {}
