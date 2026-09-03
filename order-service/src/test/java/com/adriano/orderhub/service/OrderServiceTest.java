package com.adriano.orderhub.service;

import com.adriano.orderhub.domain.order.Order;
import com.adriano.orderhub.domain.order.OrderStatus;
import com.adriano.orderhub.dto.order.OrderItemRequest;
import com.adriano.orderhub.dto.order.OrderRequest;
import com.adriano.orderhub.domain.order.OrderItem;
import com.adriano.orderhub.integration.catalog.client.CatalogClient;
import com.adriano.orderhub.integration.catalog.dto.CatalogProductResponse;
import com.adriano.orderhub.integration.catalog.dto.StockAdjustmentRequest;
import com.adriano.orderhub.kafka.KafkaEventPublisher;
import com.adriano.orderhub.mapper.order.OrderMapper;
import com.adriano.orderhub.repository.order.OrderRepository;
import com.adriano.orderhub.service.order.OrderService;
import feign.FeignException;
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
        savedOrder.setCustomerId("customer-123");
        savedOrder.setStatus(OrderStatus.PENDING_PAYMENT);
        savedOrder.setTotalAmount(new BigDecimal("6000.00"));
        savedOrder.setCreatedAt(LocalDateTime.now());

        when(catalogClient.getProductById(productId)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        var response = orderService.createOrder(request, "customer-123", "customer-123@example.com");

        assertThat(response.customerId()).isEqualTo("customer-123");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(orderRepository).save(any(Order.class));
        verify(kafkaEventPublisher).publish(eq("order-events"), any(), eq("order.created.v1"), any());
        verify(catalogClient).decreaseStock(eq(productId), eq(new StockAdjustmentRequest(2)));
    }

    @Test
    void createOrder_shouldThrowWhenProductIsInactive() {
        var productId = "prod-inactive";
        var request = new OrderRequest(List.of(new OrderItemRequest(productId, 1)));
        var inactiveProduct = new CatalogProductResponse(productId, "Item Inativo", new BigDecimal("100.00"), false);

        when(catalogClient.getProductById(productId)).thenReturn(inactiveProduct);

        assertThatThrownBy(() -> orderService.createOrder(request, "customer-123", "customer-123@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item Inativo");

        verify(orderRepository, never()).save(any());
        verify(kafkaEventPublisher, never()).publish(any(), any(), any(), any());
        verify(catalogClient, never()).decreaseStock(any(), any());
    }

    @Test
    void createOrder_shouldRollbackDecrementedStockWhenLaterItemHasInsufficientStock() {
        var productA = "prod-a";
        var productB = "prod-b";
        var request = new OrderRequest(List.of(
                new OrderItemRequest(productA, 2),
                new OrderItemRequest(productB, 3)
        ));
        var itemA = new CatalogProductResponse(productA, "Item A", new BigDecimal("100.00"), true);
        var itemB = new CatalogProductResponse(productB, "Item B", new BigDecimal("50.00"), true);

        when(catalogClient.getProductById(productA)).thenReturn(itemA);
        when(catalogClient.getProductById(productB)).thenReturn(itemB);
        doNothing().when(catalogClient).decreaseStock(eq(productA), any());
        doThrow(mock(FeignException.Conflict.class))
                .when(catalogClient).decreaseStock(eq(productB), any());

        assertThatThrownBy(() -> orderService.createOrder(request, "customer-123", "customer-123@example.com"))
                .isInstanceOf(FeignException.Conflict.class);

        verify(catalogClient).decreaseStock(eq(productA), eq(new StockAdjustmentRequest(2)));
        verify(catalogClient).increaseStock(eq(productA), eq(new StockAdjustmentRequest(2)));
        verify(catalogClient, never()).increaseStock(eq(productB), any());
        verify(orderRepository, never()).save(any());
        verify(kafkaEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void listOrdersForCustomer_shouldReturnOnlyThatCustomersOrders() {
        var order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId("customer-123");
        order.setStatus(OrderStatus.PAID);
        order.setTotalAmount(new BigDecimal("500.00"));
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc("customer-123")).thenReturn(List.of(order));

        var responses = orderService.listOrdersForCustomer("customer-123");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).customerId()).isEqualTo("customer-123");
        verify(orderRepository).findByCustomerIdOrderByCreatedAtDesc("customer-123");
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
    void updateOrderStatus_shouldReleaseStockWhenCancelled() {
        var orderId = UUID.randomUUID();
        var order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        var itemA = new OrderItem();
        itemA.setProductId("prod-a");
        itemA.setQuantity(2);
        var itemB = new OrderItem();
        itemB.setProductId("prod-b");
        itemB.setQuantity(3);
        order.getItems().add(itemA);
        order.getItems().add(itemB);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(catalogClient).increaseStock(eq("prod-a"), eq(new StockAdjustmentRequest(2)));
        verify(catalogClient).increaseStock(eq("prod-b"), eq(new StockAdjustmentRequest(3)));
    }

    @Test
    void updateOrderStatus_shouldNotReleaseStockWhenTransitioningToPaid() {
        var orderId = UUID.randomUUID();
        var order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.updateOrderStatus(orderId, OrderStatus.PAID);

        verify(catalogClient, never()).increaseStock(any(), any());
    }

    @Test
    void updateOrderStatus_shouldStillCommitCancellationWhenStockReleaseFails() {
        var orderId = UUID.randomUUID();
        var order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        var item = new OrderItem();
        item.setProductId("prod-a");
        item.setQuantity(2);
        order.getItems().add(item);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        doThrow(new RuntimeException("catalog down")).when(catalogClient).increaseStock(any(), any());

        orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }
}
