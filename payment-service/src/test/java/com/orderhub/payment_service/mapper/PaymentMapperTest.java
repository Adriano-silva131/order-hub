package com.orderhub.payment_service.mapper;

import com.orderhub.payment_service.domain.payment.PaymentStatus;
import com.orderhub.payment_service.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Test
    void toEntity_shouldApproveWhenAmountIsBelowThreshold() {
        var event = new OrderCreatedEvent(UUID.randomUUID(), "customer-1", new BigDecimal("9999.99"));

        var payment = mapper.toEntity(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getOrderId()).isEqualTo(event.orderId());
        assertThat(payment.getCustomerId()).isEqualTo("customer-1");
        assertThat(payment.getAmount()).isEqualByComparingTo("9999.99");
    }

    @Test
    void toEntity_shouldRejectWhenAmountIsAboveThreshold() {
        var event = new OrderCreatedEvent(UUID.randomUUID(), "customer-1", new BigDecimal("10000.01"));

        var payment = mapper.toEntity(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    }

    @Test
    void toEntity_shouldApproveWhenAmountIsExactlyAtThreshold() {
        var event = new OrderCreatedEvent(UUID.randomUUID(), "customer-1", new BigDecimal("10000.00"));

        var payment = mapper.toEntity(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }
}
