package com.adriano.orderhub.integration.catalog.client;

import com.adriano.orderhub.integration.catalog.dto.CatalogProductResponse;
import com.adriano.orderhub.integration.catalog.dto.StockAdjustmentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "catalog-service", url = "${integration.catalog.url}")
public interface CatalogClient {

    @GetMapping("/{id}")
    CatalogProductResponse getProductById(@PathVariable String id);

    @PostMapping("/{id}/stock/decrease")
    void decreaseStock(@PathVariable String id, @RequestBody StockAdjustmentRequest request);

    @PostMapping("/{id}/stock/increase")
    void increaseStock(@PathVariable String id, @RequestBody StockAdjustmentRequest request);
}
