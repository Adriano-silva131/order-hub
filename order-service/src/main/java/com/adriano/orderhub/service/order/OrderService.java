package com.adriano.orderhub.service.order;

import com.adriano.orderhub.domain.order.Order;
import com.adriano.orderhub.domain.order.OrderItem;
import com.adriano.orderhub.domain.order.OrderStatus;
import com.adriano.orderhub.dto.order.OrderItemRequest;
import com.adriano.orderhub.dto.order.OrderRequest;
import com.adriano.orderhub.dto.order.OrderResponse;
import com.adriano.orderhub.event.OrderCreatedEvent;
import com.adriano.orderhub.integration.catalog.client.CatalogClient;
import com.adriano.orderhub.integration.catalog.dto.CatalogProductResponse;
import com.adriano.orderhub.integration.catalog.dto.StockAdjustmentRequest;
import com.adriano.orderhub.kafka.KafkaEventPublisher;
import com.adriano.orderhub.mapper.order.OrderMapper;
import com.adriano.orderhub.repository.order.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final OrderMapper orderMapper;
    private final KafkaEventPublisher kafkaEventPublisher;

    public OrderService(OrderRepository orderRepository, CatalogClient catalogClient, OrderMapper orderMapper, KafkaEventPublisher kafkaEventPublisher) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.orderMapper = orderMapper;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request, String customerId, String customerEmail) {
        Order order = orderMapper.toEntity(customerId, customerEmail);

        BigDecimal totalAmount = buildOrderItems(order, request);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        OrderCreatedEvent event = orderMapper.toEvent(savedOrder);
        kafkaEventPublisher.publish("order-events", savedOrder.getId().toString(), "order.created.v1", event);

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrdersForCustomer(String customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Order {} is already in final status {} — ignoring transition to {}", orderId, order.getStatus(), status);
            return;
        }

        order.setStatus(status);
        orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, status);

        if (status == OrderStatus.CANCELLED) {
            releaseStockForOrder(order);
        }
    }

    private record ReservedItem(String productId, int quantity) {
    }

    private BigDecimal buildOrderItems(Order order, OrderRequest request) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ReservedItem> reserved = new ArrayList<>();

        try {
            for (OrderItemRequest itemRequest : request.items()) {
                CatalogProductResponse product = fetchAndValidateProduct(itemRequest.productId());

                catalogClient.decreaseStock(itemRequest.productId(), new StockAdjustmentRequest(itemRequest.quantity()));
                reserved.add(new ReservedItem(itemRequest.productId(), itemRequest.quantity()));

                OrderItem orderItem = orderMapper.toOrderItem(itemRequest, product);
                orderItem.setOrder(order);
                order.getItems().add(orderItem);

                totalAmount = totalAmount.add(orderItem.getSubtotal());
            }
        } catch (RuntimeException ex) {
            releaseReservedStock(reserved);
            throw ex;
        }

        return totalAmount;
    }

    private CatalogProductResponse fetchAndValidateProduct(String productId) {
        CatalogProductResponse product = catalogClient.getProductById(productId);

        if (!product.active()) {
            throw new IllegalArgumentException("Product " + product.name() + " is not available for sale.");
        }

        return product;
    }

    private void releaseReservedStock(List<ReservedItem> reserved) {
        for (ReservedItem item : reserved) {
            try {
                catalogClient.increaseStock(item.productId(), new StockAdjustmentRequest(item.quantity()));
            } catch (Exception releaseEx) {
                log.error("Failed to release reserved stock for product {} qty {} during order rollback: {}",
                        item.productId(), item.quantity(), releaseEx.getMessage(), releaseEx);
            }
        }
    }

    private void releaseStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            try {
                catalogClient.increaseStock(item.getProductId(), new StockAdjustmentRequest(item.getQuantity()));
            } catch (Exception ex) {
                log.error("Failed to release stock for cancelled order {} product {} qty {}: {}",
                        order.getId(), item.getProductId(), item.getQuantity(), ex.getMessage(), ex);
            }
        }
    }
}
