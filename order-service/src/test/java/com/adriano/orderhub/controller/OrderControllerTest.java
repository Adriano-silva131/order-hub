package com.adriano.orderhub.controller;

import com.adriano.orderhub.controller.order.OrderController;
import com.adriano.orderhub.domain.order.OrderStatus;
import com.adriano.orderhub.dto.order.OrderResponse;
import com.adriano.orderhub.service.order.OrderService;
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
        var response = new OrderResponse(orderId, "customer-1", OrderStatus.PENDING_PAYMENT, new BigDecimal("300.00"), LocalDateTime.now());

        when(orderService.createOrder(any(), eq("customer-1"), any())).thenReturn(response);

        var body = """
                {
                    "items": [{ "productId": "prod-1", "quantity": 1 }]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "customer-1")
                        .header("X-User-Email", "customer-1@example.com")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "customer-1")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listOrders_shouldReturnOnlyOrdersForAuthenticatedUser() throws Exception {
        var response = new OrderResponse(UUID.randomUUID(), "customer-1", OrderStatus.PAID, new BigDecimal("150.00"), LocalDateTime.now());

        when(orderService.listOrdersForCustomer("customer-1")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/orders")
                        .header("X-User-Id", "customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerId").value("customer-1"));
    }

    @Test
    void listOrders_shouldReturn400WhenUserIdHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isBadRequest());
    }
}
