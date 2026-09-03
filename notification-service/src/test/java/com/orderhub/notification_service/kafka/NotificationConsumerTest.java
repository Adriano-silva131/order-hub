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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private EmailSenderService emailService;

    @InjectMocks
    private NotificationConsumer consumer;

    @Test
    void consumeOrderCreatedEvent_shouldSendEmailToCustomerAddress() {
        var orderId = UUID.randomUUID();
        var event = new OrderCreatedEvent(orderId, "customer-1", "customer@example.com", new BigDecimal("1500.00"));

        consumer.consumeOrderCreatedEvent(event);

        var toCaptor = ArgumentCaptor.forClass(String.class);
        var subjectCaptor = ArgumentCaptor.forClass(String.class);
        var bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendEmail(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo("customer@example.com");
        assertThat(subjectCaptor.getValue()).contains("pedido");
        assertThat(bodyCaptor.getValue()).contains(orderId.toString());
        assertThat(bodyCaptor.getValue()).contains("R$");
    }

    @Test
    void consumeOrderCreatedEvent_shouldFallBackToDemoRecipientWhenEmailIsBlank() {
        var orderId = UUID.randomUUID();
        var event = new OrderCreatedEvent(orderId, "customer-1", "", new BigDecimal("1500.00"));

        consumer.consumeOrderCreatedEvent(event);

        verify(emailService).sendEmail(anyString(), anyString(), anyString());
        var toCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmail(toCaptor.capture(), anyString(), anyString());
        assertThat(toCaptor.getValue()).isEqualTo("demo@example.com");
    }

    @Test
    void consumePaymentEvent_shouldSendApprovalEmailToCustomerAddress() {
        var orderId = UUID.randomUUID();
        var event = new PaymentResultEvent(orderId, "APPROVED", "customer@example.com");

        consumer.consumePaymentEvent(event);

        var toCaptor = ArgumentCaptor.forClass(String.class);
        var subjectCaptor = ArgumentCaptor.forClass(String.class);
        var bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendEmail(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo("customer@example.com");
        assertThat(subjectCaptor.getValue()).contains("aprovado");
        assertThat(bodyCaptor.getValue()).contains(orderId.toString());
    }

    @Test
    void consumePaymentEvent_shouldSendRejectionEmailWhenNotApproved() {
        var orderId = UUID.randomUUID();
        var event = new PaymentResultEvent(orderId, "REJECTED", "customer@example.com");

        consumer.consumePaymentEvent(event);

        var subjectCaptor = ArgumentCaptor.forClass(String.class);
        var bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendEmail(anyString(), subjectCaptor.capture(), bodyCaptor.capture());

        assertThat(subjectCaptor.getValue()).contains("Problema");
        assertThat(bodyCaptor.getValue()).contains(orderId.toString());
    }
}
