package com.adriano.orderhub.mapper.order;

import com.adriano.orderhub.domain.order.Order;
import com.adriano.orderhub.domain.order.OrderItem;
import com.adriano.orderhub.dto.order.OrderItemRequest;
import com.adriano.orderhub.dto.order.OrderRequest;
import com.adriano.orderhub.dto.order.OrderResponse;
import com.adriano.orderhub.integration.catalog.dto.CatalogProductResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderMapper {

    public Order toEntity(OrderRequest request) {
        Order order = new Order();
        order.setCustomerId(request.customerId());
        return order;
    }

    public OrderItem toOrderItem(OrderItemRequest itemRequest, CatalogProductResponse product) {
        BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(itemRequest.productId());
        orderItem.setQuantity(itemRequest.quantity());
        orderItem.setUnitPrice(product.price());
        orderItem.setSubtotal(subtotal);
        return orderItem;
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}

