package com.adriano.orderhub.controller;

import com.adriano.orderhub.controller.order.OrderController;
import com.adriano.orderhub.domain.order.OrderStatus;
import com.adriano.orderhub.dto.order.OrderResponse;
import com.adriano.orderhub.service.order.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrder_shouldReturn201WithLocationHeader() throws Exception {
        var orderId = UUID.randomUUID();
        var response = new OrderResponse(orderId, 1000L, "customer-1", OrderStatus.PENDING_PAYMENT, new BigDecimal("300.00"), List.of(), LocalDateTime.now());

        when(orderService.createOrder(any(), any(), any())).thenReturn(response);

        var body = """
                {
                    "items": [{ "productId": "prod-1", "quantity": 1 }]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", "customer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(orderId.toString())));
    }

    @Test
    void createOrder_shouldReturn400WhenUserIdHeaderIsMissing() throws Exception {
        var body = """
                {
                    "items": [{ "productId": "prod-1", "quantity": 1 }]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn400WhenItemsIsEmpty() throws Exception {
        var body = """
                {
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", "customer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listOrders_shouldReturn200WithCustomerOrders() throws Exception {
        var orderId = UUID.randomUUID();
        var response = new OrderResponse(orderId, 1000L, "customer-1", OrderStatus.PENDING_PAYMENT, new BigDecimal("300.00"), List.of(), LocalDateTime.now());

        when(orderService.listOrdersForCustomer("customer-1")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/orders")
                        .header("X-User-Id", "customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value(1000))
                .andExpect(jsonPath("$[0].id").value(orderId.toString()));
    }

    @Test
    void getOrder_shouldReturn200WithOrderNumber() throws Exception {
        var orderId = UUID.randomUUID();
        var response = new OrderResponse(orderId, 1000L, "customer-1", OrderStatus.PENDING_PAYMENT, new BigDecimal("300.00"), List.of(), LocalDateTime.now());

        when(orderService.getOrder(eq("customer-1"), eq(orderId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("X-User-Id", "customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(1000))
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void getOrder_shouldReturn404WhenOrderNotFoundOrNotOwned() throws Exception {
        var orderId = UUID.randomUUID();

        when(orderService.getOrder(eq("customer-1"), eq(orderId)))
                .thenThrow(new EntityNotFoundException("Order not found: " + orderId));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("X-User-Id", "customer-1"))
                .andExpect(status().isNotFound());
    }
}
