package com.adriano.catalogservice.service;

import com.adriano.catalogservice.domain.product.Product;
import com.adriano.catalogservice.dto.product.ProductRequest;
import com.adriano.catalogservice.dto.product.ProductResponse;
import com.adriano.catalogservice.exception.product.ProductNotFoundException;
import com.adriano.catalogservice.mapper.product.ProductMapper;
import com.adriano.catalogservice.repository.product.ProductRepository;
import com.adriano.catalogservice.service.product.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper mapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void findAllActive_shouldReturnMappedProducts() {
        var product = activeProduct("1", "Teclado");
        var response = new ProductResponse("1", "Teclado", "Desc", new BigDecimal("200.00"), Map.of(), true);

        when(productRepository.findByActiveTrue()).thenReturn(List.of(product));
        when(mapper.toResponse(product)).thenReturn(response);

        var result = productService.findAllActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Teclado");
    }

    @Test
    void findById_shouldReturnProductWhenActiveAndFound() {
        var product = activeProduct("1", "Monitor");
        var response = new ProductResponse("1", "Monitor", "Desc", new BigDecimal("1500.00"), Map.of(), true);

        when(productRepository.findById("1")).thenReturn(Optional.of(product));
        when(mapper.toResponse(product)).thenReturn(response);

        var result = productService.findById("1");

        assertThat(result.name()).isEqualTo("Monitor");
    }

    @Test
    void findById_shouldThrowWhenProductIsInactive() {
        var product = inactiveProduct("2", "Produto Descontinuado");
        when(productRepository.findById("2")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.findById("2"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void findById_shouldThrowWhenProductNotFound() {
        when(productRepository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById("99"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_shouldSaveAndReturnMappedProduct() {
        var request = new ProductRequest("Mouse", "Mouse sem fio", new BigDecimal("150.00"), true, Map.of());
        var entity = activeProduct("3", "Mouse");
        var response = new ProductResponse("3", "Mouse", "Mouse sem fio", new BigDecimal("150.00"), Map.of(), true);

        when(mapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        var result = productService.create(request);

        assertThat(result.id()).isEqualTo("3");
        assertThat(result.name()).isEqualTo("Mouse");
        verify(productRepository).save(entity);
    }

    private Product activeProduct(String id, String name) {
        var p = new Product();
        p.setId(id);
        p.setName(name);
        p.setActive(true);
        return p;
    }

    private Product inactiveProduct(String id, String name) {
        var p = new Product();
        p.setId(id);
        p.setName(name);
        p.setActive(false);
        return p;
    }
}
