package com.adriano.orderhub.mapper.order;

import com.adriano.orderhub.domain.order.Order;
import com.adriano.orderhub.domain.order.OrderItem;
import com.adriano.orderhub.dto.order.OrderItemRequest;
import com.adriano.orderhub.dto.order.OrderItemResponse;
import com.adriano.orderhub.dto.order.OrderResponse;
import com.adriano.orderhub.event.OrderCreatedEvent;
import com.adriano.orderhub.integration.catalog.dto.CatalogProductResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class OrderMapper {

    public Order toEntity(String customerId) {
        Order order = new Order();
        order.setCustomerId(customerId);
        return order;
    }

    public OrderItem toOrderItem(OrderItemRequest itemRequest, CatalogProductResponse product) {
        BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(itemRequest.productId());
        orderItem.setProductName(product.name());
        orderItem.setQuantity(itemRequest.quantity());
        orderItem.setUnitPrice(product.price());
        orderItem.setSubtotal(subtotal);
        return orderItem;
    }

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt()
        );
    }

    public OrderCreatedEvent toEvent(Order order, String customerEmail) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                customerEmail,
                order.getTotalAmount(),
                order.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}

