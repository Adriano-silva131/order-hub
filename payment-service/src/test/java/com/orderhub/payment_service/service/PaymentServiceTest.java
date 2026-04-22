package com.orderhub.payment_service.service;

import com.orderhub.payment_service.domain.payment.Payment;
import com.orderhub.payment_service.domain.payment.PaymentStatus;
import com.orderhub.payment_service.event.OrderCreatedEvent;
import com.orderhub.payment_service.kafka.KafkaEventPublisher;
import com.orderhub.payment_service.mapper.PaymentMapper;
import com.orderhub.payment_service.repository.payment.PaymentRepository;
import com.orderhub.payment_service.service.payment.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPayment_shouldSaveAndPublishEvent() {
        var orderId = UUID.randomUUID();
        var event = new OrderCreatedEvent(orderId, "customer-1", new BigDecimal("500.00"));

        var payment = Payment.builder()
                .orderId(orderId)
                .customerId("customer-1")
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.APPROVED)
                .build();

        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentMapper.toEntity(event)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.processPayment(event);

        verify(paymentRepository).save(payment);
        verify(kafkaEventPublisher).publish(eq("payment-events"), eq(orderId.toString()), eq("payment.processed.v1"), any());
    }

    @Test
    void processPayment_shouldBeIdempotentWhenPaymentAlreadyExists() {
        var orderId = UUID.randomUUID();
        var event = new OrderCreatedEvent(orderId, "customer-1", new BigDecimal("500.00"));

        when(paymentRepository.existsByOrderId(orderId)).thenReturn(true);

        paymentService.processPayment(event);

        verify(paymentRepository, never()).save(any());
        verify(kafkaEventPublisher, never()).publish(any(), any(), any(), any());
    }
}
