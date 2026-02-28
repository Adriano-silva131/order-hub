package com.adriano.orderhub.service.order;

import com.adriano.orderhub.domain.order.Order;
import com.adriano.orderhub.domain.order.OrderItem;
import com.adriano.orderhub.dto.order.OrderItemRequest;
import com.adriano.orderhub.dto.order.OrderRequest;
import com.adriano.orderhub.dto.order.OrderResponse;
import com.adriano.orderhub.event.OrderCreatedEvent;
import com.adriano.orderhub.integration.catalog.client.CatalogClient;
import com.adriano.orderhub.integration.catalog.dto.CatalogProductResponse;
import com.adriano.orderhub.kafka.KafkaEventPublisher;
import com.adriano.orderhub.mapper.order.OrderMapper;
import com.adriano.orderhub.repository.order.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
    public OrderResponse createOrder(OrderRequest request) {
        Order order = orderMapper.toEntity(request);

        BigDecimal totalAmount = buildOrderItems(order, request);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        OrderCreatedEvent event = orderMapper.toEvent(savedOrder);
        kafkaEventPublisher.publish("order-events", savedOrder.getId().toString(), "order.created.v1", event);

        return orderMapper.toResponse(savedOrder);
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
