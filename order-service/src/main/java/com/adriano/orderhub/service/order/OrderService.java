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
import com.adriano.orderhub.kafka.KafkaEventPublisher;
import com.adriano.orderhub.mapper.order.OrderMapper;
import com.adriano.orderhub.repository.order.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    public OrderResponse createOrder(String customerId, String customerEmail, OrderRequest request) {
        Order order = orderMapper.toEntity(customerId);

        BigDecimal totalAmount = buildOrderItems(order, request);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.saveAndFlush(order);

        OrderCreatedEvent event = orderMapper.toEvent(savedOrder, customerEmail);
        kafkaEventPublisher.publish("order-events", savedOrder.getId().toString(), "order.created.v1", event);

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(String customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String customerId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getCustomerId().equals(customerId))
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        return orderMapper.toResponse(order);
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
    }

    private BigDecimal buildOrderItems(Order order, OrderRequest request) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {
            CatalogProductResponse product = fetchAndValidateProduct(itemRequest.productId());

            OrderItem orderItem = orderMapper.toOrderItem(itemRequest, product);
            orderItem.setOrder(order);
            order.getItems().add(orderItem);

            totalAmount = totalAmount.add(orderItem.getSubtotal());
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
}
