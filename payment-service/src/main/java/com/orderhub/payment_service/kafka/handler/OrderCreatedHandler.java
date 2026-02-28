package com.orderhub.payment_service.kafka.handler;

import com.orderhub.payment_service.event.OrderCreatedEvent;
import com.orderhub.payment_service.kafka.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedHandler implements EventHandler<OrderCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedHandler.class);

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
        log.info("=======================================================");
        log.info("NOVO PEDIDO RECEBIDO NO PAYMENT-SERVICE!");
        log.info("ID do Pedido: {}", event.orderId());
        log.info("Cliente: {}", event.customerId());
        log.info("Valor a ser cobrado: R$ {}", event.totalAmount());
        log.info("=======================================================");
    }
}
