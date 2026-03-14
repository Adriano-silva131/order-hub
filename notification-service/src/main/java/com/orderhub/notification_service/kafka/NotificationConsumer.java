package com.orderhub.notification_service.kafka;

import com.orderhub.notification_service.event.OrderCreatedEvent;
import com.orderhub.notification_service.event.PaymentResultEvent;
import com.orderhub.notification_service.service.EmailSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final EmailSenderService emailService;

    public NotificationConsumer(EmailSenderService emailService) {
        this.emailService = emailService;
    }

    // 1. ESCUTA O PEDIDO SENDO CRIADO
    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("🛒 Evento de PEDIDO CRIADO recebido para notificação. Pedido: {}", event.orderId());

        String customerEmail = "joao.silva@cliente.com";
        String subject = "Seu pedido foi recebido! 🛍️";

        // Formata o valor para Reais
        String valorFormatado = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                .format(event.totalAmount());

        String body = String.format(
                "Olá João!\n\nRecebemos o seu pedido %s no valor de %s.\nEstamos aguardando a confirmação do pagamento para dar andamento ao envio.",
                event.orderId(), valorFormatado);

        emailService.sendEmail(customerEmail, subject, body);
    }

    // 2. ESCUTA O RESULTADO DO PAGAMENTO (O que já tínhamos feito)
    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void consumePaymentEvent(PaymentResultEvent event) {
        log.info("💳 Evento de PAGAMENTO recebido para notificação. Pedido: {}", event.orderId());

        String customerEmail = "joao.silva@cliente.com";
        String subject;
        String body;

        if ("APPROVED".equals(event.status())) {
            subject = "Pagamento Aprovado! 🎉";
            body = String.format("Boas notícias, João! O pagamento do seu pedido %s foi confirmado. Ele já está sendo separado no nosso estoque.", event.orderId());
        } else {
            subject = "Problema no pagamento do seu pedido ⚠️";
            body = String.format("Olá João. Infelizmente o pagamento do pedido %s não foi autorizado pela operadora. Por favor, acesse o site e tente outro cartão.", event.orderId());
        }

        emailService.sendEmail(customerEmail, subject, body);
    }
}