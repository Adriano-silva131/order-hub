package com.adriano.orderhub.kafka.handler;

import com.adriano.orderhub.domain.order.OrderStatus;
import com.adriano.orderhub.event.PaymentProcessedEvent;
import com.adriano.orderhub.kafka.EventHandler;
import com.adriano.orderhub.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessedHandler implements EventHandler<PaymentProcessedEvent> {

    private final OrderService orderService;

    @Override
    public String eventType() {
        return "payment.processed.v1";
    }

    @Override
    public Class<PaymentProcessedEvent> payloadType() {
        return PaymentProcessedEvent.class;
    }

    @Override
    public void handle(PaymentProcessedEvent event) {
        log.info("Received event payment.processed.v1 for order: {}", event.orderId());
        OrderStatus newStatus = "APPROVED".equals(event.status()) ? OrderStatus.PAID : OrderStatus.CANCELLED;
        orderService.updateOrderStatus(event.orderId(), newStatus);
    }
}
