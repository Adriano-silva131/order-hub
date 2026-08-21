package com.orderhub.payment_service.repository.payment;

import com.orderhub.payment_service.domain.payment.Payment;
import com.orderhub.payment_service.domain.payment.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe um PostgreSQL real via Testcontainers para validar a constraint de
 * unicidade em order_id (base da checagem de idempotência do serviço) e as
 * migrations do Flyway contra o dialeto real do Postgres, não H2.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void existsByOrderIdReflectsPersistedIdempotencyKey() {
        UUID orderId = UUID.randomUUID();

        assertThat(paymentRepository.existsByOrderId(orderId)).isFalse();

        Payment payment = Payment.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID().toString())
                .amount(new BigDecimal("50.00"))
                .status(PaymentStatus.APPROVED)
                .build();
        paymentRepository.save(payment);

        assertThat(paymentRepository.existsByOrderId(orderId)).isTrue();
    }
}
