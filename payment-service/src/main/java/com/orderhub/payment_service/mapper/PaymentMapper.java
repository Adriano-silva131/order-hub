package com.orderhub.payment_service.mapper;

import com.orderhub.payment_service.domain.payment.Payment;
import com.orderhub.payment_service.domain.payment.PaymentStatus;
import com.orderhub.payment_service.event.OrderCreatedEvent;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toEntity(OrderCreatedEvent event) {
        PaymentStatus status = event.totalAmount().doubleValue() > 10000.0
                ? PaymentStatus.REJECTED
                : PaymentStatus.APPROVED;

        return Payment.builder()
                .orderId(event.orderId())
                .customerId(event.customerId())
                .amount(event.totalAmount())
                .status(status)
                .build();
    }
}
