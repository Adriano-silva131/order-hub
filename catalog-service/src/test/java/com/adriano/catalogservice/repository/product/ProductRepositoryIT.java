package com.adriano.catalogservice.repository.product;

import com.adriano.catalogservice.domain.product.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe um MongoDB real via Testcontainers, ao contrário dos testes de
 * serviço/controller que usam mocks — valida que o schema flexível
 * (Map<String, Object> attributes) e a query findByActiveTrue funcionam
 * de verdade contra o driver/servidor Mongo real.
 */
@Testcontainers
@DataMongoTest
class ProductRepositoryIT {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findByActiveTrueReturnsOnlyActiveProductsWithFlexibleAttributes() {
        Product active = new Product("Teclado mecânico", "RGB, switches azuis", new BigDecimal("399.90"), true);
        active.addAttribute("cor", "preto");
        active.addAttribute("switches", "blue");

        Product inactive = new Product("Mouse descontinuado", "modelo antigo", new BigDecimal("59.90"), false);

        productRepository.save(active);
        productRepository.save(inactive);

        List<Product> activeProducts = productRepository.findByActiveTrue();

        assertThat(activeProducts).hasSize(1);
        assertThat(activeProducts.get(0).getName()).isEqualTo("Teclado mecânico");
        assertThat(activeProducts.get(0).getAttributes()).containsEntry("cor", "preto");
    }
}
