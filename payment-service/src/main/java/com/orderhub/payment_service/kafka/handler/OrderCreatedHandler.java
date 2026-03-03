package com.orderhub.payment_service.kafka.handler;

import com.orderhub.payment_service.event.OrderCreatedEvent;
import com.orderhub.payment_service.kafka.EventHandler;
import com.orderhub.payment_service.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedHandler implements EventHandler<OrderCreatedEvent> {

    private final PaymentService paymentService;

    @Override
    public String eventType() {
        return "order.created.v1";
    }

    @Override
    public Class<OrderCreatedEvent> payloadType() {
        return OrderCreatedEvent.class;
    }

    @Override
    public void handle(OrderCreatedEvent event) {
        log.info("Received event order.created.v1 for order: {}", event.orderId());

        if (true) throw new RuntimeException("Simulando falha no payment-service!");

        paymentService.processPayment(event);
    }
}
