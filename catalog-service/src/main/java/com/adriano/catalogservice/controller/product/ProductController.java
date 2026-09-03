package com.adriano.catalogservice.controller.product;

import com.adriano.catalogservice.dto.product.ProductRequest;
import com.adriano.catalogservice.dto.product.ProductResponse;
import com.adriano.catalogservice.dto.product.StockAdjustmentRequest;
import com.adriano.catalogservice.service.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllActive() {
        return ResponseEntity.ok(productService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request,
            @RequestHeader("X-User-Id") String sellerId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {
        List<String> roles = rolesHeader != null ? Arrays.asList(rolesHeader.split(",")) : List.of();

        ProductResponse response = productService.create(request, sellerId, roles);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{id}/stock/decrease")
    public ResponseEntity<Void> decreaseStock(@PathVariable String id, @Valid @RequestBody StockAdjustmentRequest request) {
        productService.decreaseStock(id, request.quantity());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stock/increase")
    public ResponseEntity<Void> increaseStock(@PathVariable String id, @Valid @RequestBody StockAdjustmentRequest request) {
        productService.increaseStock(id, request.quantity());
        return ResponseEntity.noContent().build();
    }
}
