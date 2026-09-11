package com.adriano.orderhub.mapper;

import com.adriano.orderhub.domain.order.Order;
import com.adriano.orderhub.domain.order.OrderItem;
import com.adriano.orderhub.domain.order.OrderStatus;
import com.adriano.orderhub.dto.order.OrderItemRequest;
import com.adriano.orderhub.integration.catalog.dto.CatalogProductResponse;
import com.adriano.orderhub.mapper.order.OrderMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void toEntity_shouldMapCustomerId() {
        var order = mapper.toEntity("customer-42");

        assertThat(order.getCustomerId()).isEqualTo("customer-42");
    }

    @Test
    void toOrderItem_shouldCalculateSubtotalCorrectly() {
        var itemRequest = new OrderItemRequest("prod-1", 3);
        var product = new CatalogProductResponse("prod-1", "Mouse", new BigDecimal("150.00"), true);

        OrderItem item = mapper.toOrderItem(itemRequest, product);

        assertThat(item.getProductId()).isEqualTo("prod-1");
        assertThat(item.getProductName()).isEqualTo("Mouse");
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("150.00");
        assertThat(item.getSubtotal()).isEqualByComparingTo("450.00");
    }

    @Test
    void toResponse_shouldMapAllFields() {
        var order = new Order();
        order.setId(UUID.randomUUID());
        order.setOrderNumber(1042L);
        order.setCustomerId("customer-99");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("900.00"));
        order.setCreatedAt(LocalDateTime.now());

        var item = new OrderItem();
        item.setProductId("prod-1");
        item.setProductName("Mouse");
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("150.00"));
        item.setSubtotal(new BigDecimal("450.00"));
        item.setOrder(order);
        order.getItems().add(item);

        var response = mapper.toResponse(order);

        assertThat(response.id()).isEqualTo(order.getId());
        assertThat(response.orderNumber()).isEqualTo(1042L);
        assertThat(response.customerId()).isEqualTo("customer-99");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.totalAmount()).isEqualByComparingTo("900.00");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("Mouse");
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("450.00");
    }

    @Test
    void toEvent_shouldMapOrderIdAndAmount() {
        var order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId("customer-1");
        order.setTotalAmount(new BigDecimal("500.00"));
        order.setCreatedAt(LocalDateTime.now());

        var event = mapper.toEvent(order, "customer-1@example.com");

        assertThat(event.orderId()).isEqualTo(order.getId());
        assertThat(event.customerId()).isEqualTo("customer-1");
        assertThat(event.customerEmail()).isEqualTo("customer-1@example.com");
        assertThat(event.totalAmount()).isEqualByComparingTo("500.00");
        assertThat(event.createdAt()).isNotNull();
    }
}
