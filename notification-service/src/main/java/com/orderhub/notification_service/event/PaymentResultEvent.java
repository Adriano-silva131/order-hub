package com.orderhub.notification_service.event;

import java.util.UUID;

public record PaymentResultEvent(
        UUID orderId,
        String status
) {}
