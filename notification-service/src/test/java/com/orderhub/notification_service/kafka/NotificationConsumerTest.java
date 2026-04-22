package com.orderhub.notification_service.kafka;

import com.orderhub.notification_service.event.OrderCreatedEvent;
import com.orderhub.notification_service.event.PaymentResultEvent;
import com.orderhub.notification_service.service.EmailSenderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private EmailSenderService emailService;

    @InjectMocks
    private NotificationConsumer consumer;

    @Test
    void consumeOrderCreatedEvent_shouldSendEmailWithOrderDetails() {
        var orderId = UUID.randomUUID();
        var event = new OrderCreatedEvent(orderId, UUID.randomUUID(), new BigDecimal("1500.00"));

        consumer.consumeOrderCreatedEvent(event);

        var subjectCaptor = ArgumentCaptor.forClass(String.class);
        var bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendEmail(any(), subjectCaptor.capture(), bodyCaptor.capture());

        assertThat(subjectCaptor.getValue()).contains("pedido");
        assertThat(bodyCaptor.getValue()).contains(orderId.toString());
        assertThat(bodyCaptor.getValue()).contains("R$");
    }

    @Test
    void consumePaymentEvent_shouldSendApprovalEmailWhenApproved() {
        var orderId = UUID.randomUUID();
        var event = new PaymentResultEvent(orderId, "APPROVED");

        consumer.consumePaymentEvent(event);

        var subjectCaptor = ArgumentCaptor.forClass(String.class);
        var bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendEmail(any(), subjectCaptor.capture(), bodyCaptor.capture());

        assertThat(subjectCaptor.getValue()).contains("aprovado");
        assertThat(bodyCaptor.getValue()).contains(orderId.toString());
    }

    @Test
    void consumePaymentEvent_shouldSendRejectionEmailWhenNotApproved() {
        var orderId = UUID.randomUUID();
        var event = new PaymentResultEvent(orderId, "REJECTED");

        consumer.consumePaymentEvent(event);

        var subjectCaptor = ArgumentCaptor.forClass(String.class);
        var bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendEmail(any(), subjectCaptor.capture(), bodyCaptor.capture());

        assertThat(subjectCaptor.getValue()).contains("Problema");
        assertThat(bodyCaptor.getValue()).contains(orderId.toString());
    }

    private static String any() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
