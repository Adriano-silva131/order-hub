package com.adriano.orderhub.integration.catalog.client;

import com.adriano.orderhub.integration.catalog.dto.CatalogProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service", url = "${integration.catalog.url}")
public interface CatalogClient {

    @GetMapping("/{id}")
    CatalogProductResponse getProductById(@PathVariable String id);
}
