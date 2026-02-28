package com.orderhub.payment_service.service.payment;

import com.orderhub.payment_service.domain.payment.Payment;
import com.orderhub.payment_service.event.OrderCreatedEvent;
import com.orderhub.payment_service.event.PaymentProcessedEvent;
import com.orderhub.payment_service.kafka.KafkaEventPublisher;
import com.orderhub.payment_service.mapper.PaymentMapper;
import com.orderhub.payment_service.repository.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        if (paymentRepository.existsByOrderId(event.orderId())) {
            log.warn("Payment already exists for orderId: {}", event.orderId());
            return;
        }

        log.info("Processing payment for orderId: {}", event.orderId());

        Payment payment = paymentMapper.toEntity(event);
        paymentRepository.save(payment);

        PaymentProcessedEvent processedEvent = new PaymentProcessedEvent(
                payment.getOrderId(),
                payment.getStatus().name()
        );

        kafkaEventPublisher.publish(
                "payment-events",
                payment.getOrderId().toString(),
                "payment.processed.v1",
                processedEvent
        );

        log.info("Payment processed and event published for orderId: {}", event.orderId());
    }
}
