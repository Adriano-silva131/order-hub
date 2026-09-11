package com.adriano.orderhub.service;

import com.adriano.orderhub.domain.order.Order;
import com.adriano.orderhub.domain.order.OrderStatus;
import com.adriano.orderhub.dto.order.OrderItemRequest;
import com.adriano.orderhub.dto.order.OrderRequest;
import com.adriano.orderhub.integration.catalog.client.CatalogClient;
import com.adriano.orderhub.integration.catalog.dto.CatalogProductResponse;
import com.adriano.orderhub.kafka.KafkaEventPublisher;
import com.adriano.orderhub.mapper.order.OrderMapper;
import com.adriano.orderhub.repository.order.OrderRepository;
import com.adriano.orderhub.service.order.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, catalogClient, new OrderMapper(), kafkaEventPublisher);
    }

    @Test
    void createOrder_shouldSaveOrderAndPublishEvent() {
        var productId = "prod-1";
        var request = new OrderRequest(List.of(new OrderItemRequest(productId, 2)));
        var product = new CatalogProductResponse(productId, "Notebook", new BigDecimal("3000.00"), true);

        var savedOrder = new Order();
        savedOrder.setId(UUID.randomUUID());
        savedOrder.setOrderNumber(1000L);
        savedOrder.setCustomerId("customer-123");
        savedOrder.setStatus(OrderStatus.PENDING_PAYMENT);
        savedOrder.setTotalAmount(new BigDecimal("6000.00"));
        savedOrder.setCreatedAt(LocalDateTime.now());

        when(catalogClient.getProductById(productId)).thenReturn(product);
        when(orderRepository.saveAndFlush(any(Order.class))).thenReturn(savedOrder);

        var response = orderService.createOrder("customer-123", "customer-123@example.com", request);

        assertThat(response.customerId()).isEqualTo("customer-123");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.orderNumber()).isEqualTo(1000L);
        verify(orderRepository).saveAndFlush(any(Order.class));
        verify(kafkaEventPublisher).publish(eq("order-events"), any(), eq("order.created.v1"), any());
    }

    @Test
    void createOrder_shouldThrowWhenProductIsInactive() {
        var productId = "prod-inactive";
        var request = new OrderRequest(List.of(new OrderItemRequest(productId, 1)));
        var inactiveProduct = new CatalogProductResponse(productId, "Item Inativo", new BigDecimal("100.00"), false);

        when(catalogClient.getProductById(productId)).thenReturn(inactiveProduct);

        assertThatThrownBy(() -> orderService.createOrder("customer-123", "customer-123@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item Inativo");

        verify(orderRepository, never()).saveAndFlush(any());
        verify(kafkaEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void updateOrderStatus_shouldUpdateWhenPending() {
        var orderId = UUID.randomUUID();
        var order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.updateOrderStatus(orderId, OrderStatus.PAID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_shouldIgnoreWhenAlreadyInFinalStatus() {
        var orderId = UUID.randomUUID();
        var order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PAID);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_shouldThrowWhenOrderNotFound() {
        var orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.PAID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    void listOrders_shouldReturnOrdersForCustomerOrderedByMostRecent() {
        var order = new Order();
        order.setId(UUID.randomUUID());
        order.setOrderNumber(1001L);
        order.setCustomerId("customer-123");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("300.00"));
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc("customer-123")).thenReturn(List.of(order));

        var response = orderService.listOrders("customer-123");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).orderNumber()).isEqualTo(1001L);
    }

    @Test
    void getOrder_shouldReturnResponseWhenCustomerOwnsOrder() {
        var orderId = UUID.randomUUID();
        var order = new Order();
        order.setId(orderId);
        order.setOrderNumber(1000L);
        order.setCustomerId("customer-123");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("300.00"));
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        var response = orderService.getOrder("customer-123", orderId);

        assertThat(response.orderNumber()).isEqualTo(1000L);
        assertThat(response.id()).isEqualTo(orderId);
    }

    @Test
    void getOrder_shouldThrowWhenOrderBelongsToAnotherCustomer() {
        var orderId = UUID.randomUUID();
        var order = new Order();
        order.setId(orderId);
        order.setCustomerId("customer-owner");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrder("customer-other", orderId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getOrder_shouldThrowWhenOrderNotFound() {
        var orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder("customer-123", orderId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
