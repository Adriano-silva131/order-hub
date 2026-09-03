package com.adriano.orderhub.controller.order;

import com.adriano.orderhub.dto.order.OrderRequest;
import com.adriano.orderhub.dto.order.OrderResponse;
import com.adriano.orderhub.service.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader("X-User-Id") String customerId,
            @RequestHeader(value = "X-User-Email", required = false) String customerEmail) {
        OrderResponse response = orderService.createOrder(request, customerId, customerEmail);

        URI location = ServletUriComponentsBuilder.
                fromCurrentRequest().
                path("/{id}").
                buildAndExpand(response.id()).
                toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(@RequestHeader("X-User-Id") String customerId) {
        return ResponseEntity.ok(orderService.listOrdersForCustomer(customerId));
    }
}
