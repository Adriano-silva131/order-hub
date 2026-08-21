package com.adriano.orderhub.repository.order;

import com.adriano.orderhub.domain.order.Order;
import com.adriano.orderhub.domain.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe um PostgreSQL real via Testcontainers e roda as migrations do Flyway
 * de verdade, ao contrário dos testes com H2 — que não pegam divergências
 * entre o schema esperado pelas migrations e o dialeto real do Postgres.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void persistsAndReloadsOrderWithDefaultsAppliedByFlywaySchema() {
        Order order = Order.builder()
                .customerId(UUID.randomUUID().toString())
                .totalAmount(new BigDecimal("199.90"))
                .build();

        Order saved = orderRepository.save(order);

        Optional<Order> reloaded = orderRepository.findById(saved.getId());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(reloaded.get().getTotalAmount()).isEqualByComparingTo("199.90");
        assertThat(reloaded.get().getCreatedAt()).isNotNull();
    }
}
